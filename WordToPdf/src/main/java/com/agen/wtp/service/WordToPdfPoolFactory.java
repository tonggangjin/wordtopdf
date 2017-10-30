package com.agen.wtp.service;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.agen.wtp.config.Config;
import com.agen.wtp.util.CommandUtil;

/**
 * 
 * <label> wordtopdf工厂. </label>
 * <p>
 * 
 * <pre>
 *			整个应用核心，wordtopdf进程创建及负载均由此类完成
 * </pre>
 * </p>
 * 
 * @ClassName WordToPdfPoolFactory
 * @author tgj
 * @date 2017年10月20日 下午3:22:57
 * 
 * @Copyright 2017 www.agen.com Inc. All rights reserved.
 */
@Service("wtpReportPoolFactory")
public class WordToPdfPoolFactory {

	private static final Logger LOG = Logger.getLogger(WordToPdfPoolFactory.class);

	@Value("${wtp.reportProgressSize}")
	private Integer reportProgressSize;

	@Value("${wtp.reportThreadSize}")
	private Integer reportThreadSize;

	@Value("${wtp.fault-tolerant}")
	private Integer faultTolerant;

	@Value("${wtp.fault-tolerantTime}")
	private Integer faultTolerantTime;

	@Value("${wtp.fault-tolerantSleepTime}")
	private Long faultTolerantSleepTime;

	@Autowired
	private Environment environment;
	
	private static final Random RD = new Random();

	public Environment getEnvironment() {
		return environment;
	}

	/**
	 * 记数器，主要用做负载
	 */
	private AtomicInteger index = new AtomicInteger(-1);

	/**
	 * word进程创建线程，主要负责创建word进程
	 */
	private Thread createProggressThread;

	/**
	 * word进程从进程池正在移除的标识
	 */
	private AtomicBoolean deleting = new AtomicBoolean(false);

	/**
	 * word进程池
	 */
	@Autowired
	@Qualifier("wtpCopyOnWriteArrayList")
	private CopyOnWriteArrayList<ReportPoolThread> copyOnWriteArrayList;
	
	@Autowired
	@Qualifier("wtpCommandUtil")
	private CommandUtil commandUtil;

	/**
	 * word进程创建控制标识
	 */
	private static final AtomicBoolean createProggressFlag = new AtomicBoolean(true);

	@PostConstruct
	public void init() {
		if (faultTolerant <= 0) {
			LOG.warn("你没有设置faultTolerant数量，将使用默认值100");
			faultTolerant = 100;
		}
		if (faultTolerantTime <= 0) {
			LOG.warn("你没有设置faultTolerantTime数量，将使用默认值100");
			faultTolerantTime = 100;
		}
		faultTolerantTime = faultTolerantTime << 5;
		if (faultTolerantSleepTime <= 0) {
			LOG.warn("你没有设置faultTolerantSleepTime数量，将使用默认值3");
			faultTolerantTime = 3;
		}
		if (reportProgressSize <= 0) {
			LOG.warn("你没有设置word进程数量，将使用默认值，创建2进程");
			reportProgressSize = 2;
		}
		if (reportThreadSize <= 0) {
			LOG.warn("你没有设置word线程数量，将使用默认值，创建3线程");
			reportThreadSize = 3;
		}
		createProggressThread = initProgressThread();
		createProggressThread.start();
	}

	private Thread initProgressThread() {
		Thread createProggressThread = new Thread(() -> {
			createConnection();
		});
		createProggressThread.setDaemon(true);
		createProggressThread.setName("createProggressThread");
		createProggressThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				LOG.error("线程" + t.getName() + "创建word进程失败", e);
			}
		});
		return createProggressThread;
	}

	/**
	 * 
	 * <label>
	 *		创建word进程.
	 * </label>
	 * <p>
	 *		<pre>
	 *			负责创建和启动word进程
	 *		</pre>
	 * </p>
	 * @Title createConnection void
	 */
	private synchronized void createConnection() {
		while (createProggressFlag.get()) {
			if (!validateRunning()) return;
			synchronized (copyOnWriteArrayList) {
				int errorTime = 0;
				for (int i = copyOnWriteArrayList.size(); i < reportProgressSize; i++) {
					if (!validateRunning()) return;
					try {
						while (deleting.get()) {
							Thread.sleep(faultTolerantSleepTime);
						}
					} catch (InterruptedException e) {
						// do nothing
					}
					List<String> pids = commandUtil.getPid();
					ReportPoolThread reportPoolThread = new ReportPoolThread(WordToPdfPoolFactory.this);
					String pid = getPid(pids);
					if (StringUtils.isBlank(pid)) {
						errorTime++;
						i--;
						if (errorTime > faultTolerant)
							throw new UnsupportedOperationException("create ReportPoolThread error");
					} else {
						copyOnWriteArrayList.add(reportPoolThread);
						Thread thread = new Thread(reportPoolThread);
						thread.setName("reportProgress_" + RD.nextInt());
						reportPoolThread.setPid(pid);
						thread.setDaemon(true);
						thread.start();
					}
				}
			}
			try {
				synchronized (createProggressThread) {
					createProggressThread.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private String getPid(List<String> pids) {
		int tempFaultTolerant = 0;
		try {
			while (tempFaultTolerant < faultTolerantTime) {
				tempFaultTolerant++;
				List<String> tempPids = commandUtil.getPid();
				tempPids.removeAll(pids);
				if (!tempPids.isEmpty()) {
					return tempPids.get(0);   //此处并不完善，直接取第一个pid返回
				} else {
					Thread.sleep(faultTolerantSleepTime);
				}
			}
		} catch (Exception e) {
			LOG.error("get pid failed", e);
			return null;
		}
		return null;
	}

	/**
	 * 
	 * <label>
	 *		获得连接.
	 * </label>
	 * <p>
	 *		<pre>
	 *			负载均衡的获得连接
	 *		</pre>
	 * </p>
	 * @Title getReportPoolThread
	 * @return ReportPoolThread
	 */
	public ReportPoolThread getReportPoolThread() {
		if (!validateRunning()) return null;
		try {
			while (deleting.get()) {
				Thread.sleep(faultTolerantSleepTime);
			}
			while (copyOnWriteArrayList.isEmpty()) {
				synchronized (createProggressThread) {
					createProggressThread.notify();
				}
				Thread.sleep(faultTolerantSleepTime);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int len = copyOnWriteArrayList.size();
		if (len == 0)
			return null;
		int tempIndex = 0;
		synchronized (index) {
			tempIndex = index.incrementAndGet();
			if (len <= tempIndex) {
				index.set(0);
				tempIndex = 0;
			}
		}
		return copyOnWriteArrayList.get(tempIndex);
	}
	
	private boolean validateRunning() {
		return Config.serverRunning.get();
	}

	/**
	 * 
	 * <label>
	 *		删除连接.
	 * </label>
	 * <p>
	 *		<pre>
	 *			删除连接并触发创建连接
	 *		</pre>
	 * </p>
	 * @Title remove
	 * @param reportPoolThread void
	 */
	public void remove(ReportPoolThread reportPoolThread) {
		if (!validateRunning()) return;
		deleting.set(true);
		copyOnWriteArrayList.remove(reportPoolThread);
		deleting.set(false);
		synchronized (createProggressThread) {
			createProggressThread.notify();
		}
		reportPoolThread.destroy();
		String pid = reportPoolThread.getPid();
		commandUtil.close(pid, faultTolerantSleepTime);
	}

	@PreDestroy
	public void close() {
		createProggressFlag.set(false);
		synchronized (createProggressThread) {
			createProggressThread.notify();
		}
	}

}
