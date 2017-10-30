package com.agen.wtp.config;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import com.agen.wtp.service.ReportPoolThread;
import com.agen.wtp.util.CommandUtil;

/**
 * 
 * <label> wtp配置文件. </label>
 * <p>
 * 
 * <pre>
 * 主要配置都由此管理
 * </pre>
 * </p>
 * 
 * @ClassName Config
 * @author TGJ
 * @date 2017年10月25日 上午11:19:24
 * 
 * @Copyright 2017 www.agen.com Inc. All rights reserved.
 */
@Configuration("wtpConfig")
public class Config {

	private static final Logger LOG = LoggerFactory.getLogger(Config.class);
	
	/**
	 * applicationContent是与可以的标识
	 */
	public static final AtomicBoolean serverRunning = new AtomicBoolean(true);

	@Value("${wtp.fault-tolerantSleepTime}")
	private Long faultTolerantSleepTime;

	/**
	 * 
	 * <label> word进程池. </label>
	 * <p>
	 * 
	 * <pre>
	 * word进程保存地方
	 * </pre>
	 * </p>
	 * 
	 * @Title getCopyOnWriteArrayList
	 * @return CopyOnWriteArrayList<ReportPoolThread>
	 */
	@Bean("wtpCopyOnWriteArrayList")
	public CopyOnWriteArrayList<ReportPoolThread> getCopyOnWriteArrayList() {
		return new CopyOnWriteArrayList<>();
	}

	/**
	 * 
	 * <label> applicationContent关闭事件. </label>
	 * <p>
	 * 
	 * <pre>
	 * 在这里进行相关资源的释放
	 * </pre>
	 * </p>
	 * 
	 * @Title getContextClosedEvent
	 * @param copyOnWriteArrayList
	 * @return ApplicationListener<ContextClosedEvent>
	 */
	@Bean("wtpApplicationListener")
	public ApplicationListener<ContextClosedEvent> getContextClosedEvent(
			@Qualifier("wtpCopyOnWriteArrayList") CopyOnWriteArrayList<ReportPoolThread> copyOnWriteArrayList,
			@Qualifier("wtpCommandUtil") CommandUtil commandUtil) {
		return new ApplicationListener<ContextClosedEvent>() {
			@Override
			public void onApplicationEvent(ContextClosedEvent event) {
				serverRunning.set(false);
				for (ReportPoolThread reportPoolThread : copyOnWriteArrayList) {
					reportPoolThread.destroy();
					// if (reportPoolThread.destroy()) { // 暂时是关闭后再执行命令以确保关闭
					String pid = reportPoolThread.getPid();
					LOG.warn("close pid:" + pid + " by command");
					commandUtil.close(pid, faultTolerantSleepTime);
					// }
				}
				copyOnWriteArrayList.clear();
			}
		};
	}

	

}
