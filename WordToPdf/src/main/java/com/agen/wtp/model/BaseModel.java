package com.agen.wtp.model;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.agen.wtp.translate.TranslateAdpter;

/**
 * 
 * <label>
 *		wordtopdf使用TranslateAdpter基本模型.
 * </label>
 * <p>
 *		<pre>
 *			此模型可以安全使用，自己可以通过继承TranslateAdpter实现更丰富的功能
 *		</pre>
 * </p>
 * @ClassName BaseModel
 * @author TGJ  
 * @date 2017年10月25日 上午11:16:18 
 *    
 * @Copyright 2017 www.agen.com Inc. All rights reserved.
 */
@Component("wtpBaseModel")
@Scope("prototype")
public class BaseModel extends TranslateAdpter {

	public BaseModel() {
		
	}

	@Override
	public void callback(TranslateStatus translateStatus) {
		System.out.println(translateStatus.toString());
	}

}
