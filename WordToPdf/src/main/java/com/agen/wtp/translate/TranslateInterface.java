package com.agen.wtp.translate;

import com.agen.wtp.translate.TranslateAdpter.TranslateStatus;

public interface TranslateInterface {
	
	Integer DOC = 0;
	Integer DOT = 1;
	Integer TXT = 2;
	Integer RTF = 6;
	Integer HTM = 8;
	Integer XML = 11;
	Integer DOCX = 12;
	Integer DOCM = 13;
	Integer DOTX = 14;
	Integer DOTM = 15;
	Integer PDF = 17;
	

	void callback(TranslateStatus translateStatus);
}
