package com.agen.wtp.controller;

import java.io.File;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import com.agen.wtp.config.Config;
import com.agen.wtp.model.BaseModel;
import com.agen.wtp.service.ReportPoolThread;
import com.agen.wtp.service.WordToPdfPoolFactory;

/**
 * 
 * <label>
 *		转换控制器.
 * </label>
 * <p>
 *		<pre>
 *			主要作流程控制和数据处理(演示)
 *		</pre>
 * </p>
 * @ClassName TranslateController
 * @author TGJ  
 * @date 2017年10月25日 上午11:42:51 
 *    
 * @Copyright 2017 www.agen.com Inc. All rights reserved.
 */
@Controller("wtpTranslateController")
public class TranslateController {
	
	@Autowired
	@Qualifier("wtpReportPoolFactory")
	private WordToPdfPoolFactory wordToPdfPoolFactory;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@PostConstruct
	public void initTest() {
//		File file = new File("c:/test");
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				try {
//					Thread.sleep(15000);
//					File[] files = file.listFiles(new FilenameFilter() {
//						@Override
//						public boolean accept(File dir, String name) {
//							// TODO Auto-generated method stub
//							return name.contains(".docx");
//						}
//					});
//					for (int i = 0; i < files.length; i++) {
//						translate(files[i]);
//					}
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}).start();
	}
	
	private boolean translate(File file) {
		if (null == file || !file.exists()) return false;
		BaseModel baseModel = initModel(file);
		if (null == baseModel) return false;
		ReportPoolThread reportPoolThread = wordToPdfPoolFactory.getReportPoolThread();
		if (null == reportPoolThread) return false;
		reportPoolThread.addTask(baseModel);
		return true;
	}
	
	private BaseModel initModel(File file) {
		if (null == file || !file.exists()) return null;
		String fromPath = file.getAbsolutePath();
		String bgId = file.getParentFile().getName();
		if (!Config.serverRunning.get()) {
			return null;
		}
		BaseModel baseModel = applicationContext.getBean("wtpBaseModel", BaseModel.class);
		baseModel.setBgId(bgId);
		baseModel.setFromPath(fromPath);
		baseModel.setToPath(fromPath.replace(".docx", ".pdf").replace(".doc", ".pdf"));
		return baseModel;
	}
}
