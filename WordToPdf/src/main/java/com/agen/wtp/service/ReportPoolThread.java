package com.agen.wtp.service;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.springframework.core.env.Environment;

import com.agen.wtp.config.Config;
import com.agen.wtp.translate.TranslateAdpter;
import com.agen.wtp.translate.TranslateAdpter.TranslateStatus;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

/**
 * 
 * <label> word进程连接. </label>
 * <p>
 * 
 * <pre>
 * wordtopdf操作都由此类提供
 * </pre>
 * </p>
 * 
 * @ClassName ReportPoolThread
 * @author tgj
 * @date 2017年10月20日 下午4:54:30
 * 
 * @Copyright 2017 www.agen.com Inc. All rights reserved.
 */
public class ReportPoolThread implements Runnable, Serializable {

	private static final Logger LOG = Logger.getLogger(ReportPoolThread.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -6413566533215518784L;

	private ExecutorService es; // word线程连接池

	/**
	 * 当前进程控制标识
	 */
	private AtomicBoolean isRunAble = new AtomicBoolean(true);

	private static final int QUITNOTSAVE = 0;

	private Long faultTolerantSleepTime;

	private WordToPdfPoolFactory wordToPdfPoolFactory;

	private Dispatch dis;

	private Integer faultTolerant;

	private ActiveXComponent axc;

	private AtomicBoolean shutdowning = new AtomicBoolean(false);

	private final Map<String, Integer> FAULTTOLERANTMAP = new ConcurrentHashMap<>();
	private final ConcurrentLinkedQueue<TranslateAdpter> RECORDS = new ConcurrentLinkedQueue<>();

	public ConcurrentLinkedQueue<String> getBlacklist() {
		return BLACKLIST;
	}

	private String pid;

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	/**
	 * 
	 * <label> 添加转换任务. </label>
	 * <p>
	 * 
	 * <pre>
	 * 通过TranslateAdpter添加转换任务至当前进程
	 * </pre>
	 * </p>
	 * 
	 * @Title addTask
	 * @param translateAdpter
	 *            void
	 */
	public void addTask(TranslateAdpter translateAdpter) {
		if (null == translateAdpter || null == translateAdpter.getBgId() || null == translateAdpter.getFromPath() && null != translateAdpter.getToPath())
			new IllegalArgumentException("translateAdpter must not null");
		RECORDS.add(translateAdpter);
		synchronized (ReportPoolThread.this) {
			ReportPoolThread.this.notify();
		}
	}

	public ReportPoolThread(WordToPdfPoolFactory wordToPdfPoolFactory) {
		this.wordToPdfPoolFactory = wordToPdfPoolFactory;
		init(wordToPdfPoolFactory.getEnvironment());
	}

	public void init(Environment environment) {
		this.faultTolerantSleepTime = environment.getProperty("wtp.fault-tolerantSleepTime", Long.class);
		this.faultTolerant = environment.getProperty("wtp.fault-tolerant", Integer.class);
		es = Executors.newFixedThreadPool(environment.getProperty("wtp.reportThreadSize", Integer.class));
		String osName = environment.getProperty("os.name");
		if ("Linux".equals(osName)) {
			linuxInit();
		} else if (osName.contains("Windows")) {
			windowInit();
		} else {
			throw new UnknownError("不支持的操作系统");
		}

	}

	private void linuxInit() {
		LOG.warn("暂时不支持Linux");
	}

	private void windowInit() {
		ComThread.InitMTA(true);
		axc = new ActiveXComponent("Word.Application");
		axc.setProperty("Visible", new Variant(false));
		axc.setProperty("ScreenUpdating", new Variant(false));
		dis = axc.getProperty("Documents").toDispatch();
	}

	// 后期支持分布式，黑名需要存放于redis里面
	private static final ConcurrentLinkedQueue<String> BLACKLIST = new ConcurrentLinkedQueue<>();

	/**
	 * 
	 * <label> 关闭此进程，并进行相关处理. </label>
	 * <p>
	 * 
	 * <pre>
	 * 
	 * </pre>
	 * </p>
	 * 
	 * @Title destroy
	 * @return boolean
	 */
	public boolean destroy() {
		notifyAllFail();
		RECORDS.clear();
		isRunAble.set(false);
		synchronized (ReportPoolThread.this) {
			ReportPoolThread.this.notify();
		}
		return closeProgress();
	}

	/**
	 * 
	 * <label> 关闭word进程. </label>
	 * <p>
	 * 
	 * <pre>
	 * 
	 * </pre>
	 * </p>
	 * 
	 * @Title closeProgress
	 * @return boolean
	 */
	private boolean closeProgress() {
		try {
			if (null != axc) {
				axc.invoke("Quit", QUITNOTSAVE);
				axc = null;
			}
			dis = null;
			return true;
		} finally {
			ComThread.Release();
		}
	}

	/**
	 * 
	 * <label> 回调当前正在处理的任务. </label>
	 * <p>
	 * 
	 * <pre>
	 * 
	 * </pre>
	 * </p>
	 * 
	 * @Title notifyAllFail void
	 */
	private void notifyAllFail() {
		while (!RECORDS.isEmpty()) {
			TranslateAdpter translateAdpter = RECORDS.poll();
			translateAdpter.callback(TranslateStatus.RETRY);
		}
	}

	@Override
	public void run() {
		while (isRunAble.get()) {
			if (!validateRunning())
				return;
			while (!RECORDS.isEmpty()) {
				if (!validateRunning())
					return;
				TranslateAdpter translateAdpter = RECORDS.poll();
				if (!es.isShutdown() && !shutdowning.get()) {
					es.submit(() -> {
						if (translate(translateAdpter)) {
							translateAdpter.callback(TranslateStatus.SUCCESS);
						} else {
							if (!shutdowning.get()) {
								shutdowning.set(true);
								errorDeal(translateAdpter);
							}
						}
					});
				}
			}
			synchronized (ReportPoolThread.this) {
				try {
					ReportPoolThread.this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private boolean validateRunning() {
		return Config.serverRunning.get();
	}

	/**
	 * 
	 * <label> 异常处理. </label>
	 * <p>
	 * 
	 * <pre>
	 *			当发送意外情况，进行的处理
	 * </pre>
	 * </p>
	 * 
	 * @Title errorDeal
	 * @param translateAdpter
	 *            void
	 */
	private void errorDeal(TranslateAdpter translateAdpter) {
		isRunAble.set(false);
		es.shutdownNow();
		wordToPdfPoolFactory.remove(ReportPoolThread.this);
		Integer num = FAULTTOLERANTMAP.get(translateAdpter.getBgId());
		if (null != num) {
			if (num >= faultTolerant) {
				BLACKLIST.add(translateAdpter.getBgId());
				translateAdpter.callback(TranslateStatus.FAIL);
			} else {
				FAULTTOLERANTMAP.put(translateAdpter.getBgId(), ++num);
			}
		} else {
			FAULTTOLERANTMAP.put(translateAdpter.getBgId(), 1);
		}
		translateAdpter.callback(TranslateStatus.RETRY);
	}

	/**
	 * 
	 * <label> 转换. </label>
	 * <p>
	 * 
	 * <pre>
	 * 通过指定文件时行转换
	 * </pre>
	 * </p>
	 * 
	 * @Title translate
	 * @param filePath
	 * @return boolean
	 */
	private boolean translate(TranslateAdpter translateAdpter) {
		Dispatch doc = null;
		try {
			doc = Dispatch.call(dis, "Open", translateAdpter.getFromPath(), new Variant(false), new Variant(true)).toDispatch();
			int i = 0;
			while (i < 100) {
				try {
					Dispatch.call(doc, "SaveAs", translateAdpter.getToPath(), new Variant(translateAdpter.getSaveFormat()));
					break;
				} catch (Exception e) {
					LOG.warn("达到最大IO", e);
				}
				i++;
				Thread.sleep(faultTolerantSleepTime * 10);
			}
			Dispatch.call(doc, "Close", new Variant(false));
			doc = null;
			return i != 100;
		} catch (Exception e) {
			LOG.error("word to pdf error, filePath : " + translateAdpter, e);
			return false;
		}
	}
}
