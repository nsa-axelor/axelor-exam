package com.axelor.apps.admission.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.admission.service.AdmissionProcessService;
import com.axelor.apps.admission.service.AdmissionProcessServiceImpl;

public class AdmissionModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(AdmissionProcessService.class).to(AdmissionProcessServiceImpl.class);
  }
}
