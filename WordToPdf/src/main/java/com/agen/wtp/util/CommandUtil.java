package com.agen.wtp.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 
 * <label>
 *		命令执行工具.
 * </label>
 * <p>
 *		<pre>
 *			按给定命令执行
 *		</pre>
 * </p>
 * @ClassName CommandUtil
 * @author TGJ  
 * @date 2017年10月25日 上午11:25:21 
 *    
 * @Copyright 2017 www.agen.com Inc. All rights reserved.
 */
@Component("wtpCommandUtil")
public class CommandUtil {
	
	@Value("#{systemProperties['os.name']}")
	private String osName;
	
	/**
	 * 
	 * <label> 按倒入的命令执行. </label>
	 * <p>
	 * 
	 * <pre>
	 * 		按给定命令执行，并返回得到的结果
	 * </pre>
	 * </p>
	 * 
	 * @Title execute
	 * @param command 命令数组
	 * @param isWait 是否等待完成
	 * @return List<String>
	 * @throws IOException
	 */
	public List<String> execute(String[] command, boolean isWait) throws IOException {
		Process process = new ProcessBuilder(command).start();
		process.getOutputStream().close();
		List<String> lines = IOUtils.readLines(process.getInputStream(), "UTF-8");
		try {
			if (isWait) {
				process.waitFor();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return lines;
	}
	
	/**
	 * 
	 * <label>
	 *		关闭word进程.
	 * </label>
	 * <p>
	 *		<pre>
	 *			按pid进行word进程的关闭
	 *		</pre>
	 * </p>
	 * @Title close
	 * @param commandUtil
	 * @param pid void
	 */
	public void close(String pid, Long faultTolerantSleepTime) {
		// 默认windows
		String[] close = { "wmic", "process", pid, "delete" };
		String[] find = { "wmic", "process", "where", "processid='" + pid + "'", "get", "name" };
		if ("Linux".equals(osName)) {
			//暂时还未处理
		}
		try {
			do {
				Thread.sleep(faultTolerantSleepTime);
				execute(close, false);
			} while (!execute(find, false).stream().filter(item -> StringUtils.isNotBlank(item.trim()))
					.collect(Collectors.toList()).isEmpty());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public List<String> getPid() {
		String[] command = { "wmic", "process", "where", "name='winword.exe'", "get", "Handle" };
		try {
			if ("Linux".equals(osName)) {
				return new ArrayList<>();
			} else if (osName.contains("Windows")) {
				return execute(command, false).stream().filter(item -> !(StringUtils.isBlank(item) || "Handle".equals(item.trim())))
						.map(item ->item.trim()).collect(Collectors.toList());
			} else {
				return new ArrayList<>();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return new ArrayList<>();
		}
	
	}
}
