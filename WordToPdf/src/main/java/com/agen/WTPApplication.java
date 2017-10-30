package com.agen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 
 * <label>
 *		启动类.
 * </label>
 * <p>
 *		<pre>
 *			
 *		</pre>
 * </p>
 * @ClassName WTPApplication
 * @author tgj  
 * @date 2017年10月30日 上午9:17:29 
 *    
 * @Copyright 2017 www.agen.com Inc. All rights reserved.
 */
@SpringBootApplication
public class WTPApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication app = new SpringApplication(WTPApplication.class);
		app.setWebEnvironment(false);
		app.run(args);
		synchronized (WTPApplication.class) {
			WTPApplication.class.wait();
		}
	}
}
