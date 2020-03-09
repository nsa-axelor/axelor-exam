package com.axelor.apps.admission.web;

import com.axelor.apps.admission.db.AdmissionProcess;
import com.axelor.apps.admission.service.AdmissionProcessService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class AdmissionProcessController {

	public void checkAllAdmissionEntries(ActionRequest request, ActionResponse response) {
		try {
		AdmissionProcess admissionProcess = request.getContext().asType(AdmissionProcess.class);
		AdmissionProcessService processService = Beans.get(AdmissionProcessService.class);
		processService.setAllAdmissions(admissionProcess);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
