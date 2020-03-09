package com.axelor.apps.admission.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.builder.CompareToBuilder;

import com.axelor.apps.admission.db.AdmissionEntry;
import com.axelor.apps.admission.db.AdmissionProcess;
import com.axelor.apps.admission.db.College;
import com.axelor.apps.admission.db.CollegeEntry;
import com.axelor.apps.admission.db.FacultyEntry;
import com.axelor.apps.admission.db.repo.AdmissionEntryRepository;
import com.axelor.apps.admission.db.repo.FacultyEntryRepository;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;

public class AdmissionProcessServiceImpl implements AdmissionProcessService {

//	@Inject
//	AdmissionEntryRepository admissionEntryRepository;

//	@Inject
//	FacultyEntryRepository facultyEntryRepository;

	boolean isCollegeSelected = false;

	@Override
	public void setAllAdmissions(AdmissionProcess admissionProcess) {

		List<AdmissionEntry> admissionEntries = Beans.get(AdmissionEntryRepository.class).all()
				.filter("self.registrationDate > :fromDate AND self.registrationDate < :toDate")
				.bind("fromDate", admissionProcess.getFromDate()).bind("toDate", admissionProcess.getToDate()).fetch();

		Collections.sort((List<AdmissionEntry>) admissionEntries, new Comparator<AdmissionEntry>() {
			@Override
			public int compare(AdmissionEntry o1, AdmissionEntry o2) {
				return new CompareToBuilder().append(o2.getMerit(), o1.getMerit())
						.append(o1.getRegistrationDate(), o2.getRegistrationDate()).toComparison();
			}
		});

		admissionEntries.forEach(entry -> {
			List<CollegeEntry> collegeEntries = entry.getCollegeEntryList();

			Collections.sort((List<CollegeEntry>) collegeEntries, new Comparator<CollegeEntry>() {
				@Override
				public int compare(CollegeEntry o1, CollegeEntry o2) {
					return o1.getSequence().compareTo(o2.getSequence());
				}
			});

			collegeEntries.forEach(collegeEntry -> {
				College college = collegeEntry.getCollege();
				college.getFacultyEntryList().forEach(facultyEntry -> {
					if (entry.getFaculty().getName().equals(facultyEntry.getFaculty().getName())
							&& facultyEntry.getSeats() > 0
							&& entry.getStatusSelect() != AdmissionEntryRepository.STATUS_ADMITTED) {
						if (!isCollegeSelected) {
							entry.setCollegeSelected(college);
							isCollegeSelected = true;
							int cnt = facultyEntry.getSeats();
							cnt--;
							facultyEntry.setSeats(cnt);
							this.saveFacultyEntry(facultyEntry);
//							facultyEntryRepository.save(facultyEntry);
						}
						entry.setValidationDate(LocalDate.now());
						entry.setStatusSelect(AdmissionEntryRepository.STATUS_ADMITTED);
					} else {
						if (entry.getFaculty().getName().equals(facultyEntry.getFaculty().getName())
								&& facultyEntry.getSeats() <= 0
								&& entry.getStatusSelect() != AdmissionEntryRepository.STATUS_CANCELLED
								&& entry.getStatusSelect() != AdmissionEntryRepository.STATUS_ADMITTED) {
							entry.setStatusSelect(AdmissionEntryRepository.STATUS_CANCELLED);
						}
					}
				});
			});
			this.saveAdmissionEntry(entry);
//			admissionEntryRepository.save(entry);
			isCollegeSelected = false;
		});
	}
	
	@Transactional
	public void saveAdmissionEntry(AdmissionEntry admissionEntry) {
		Beans.get(AdmissionEntryRepository.class).save(admissionEntry);
	}
	
	@Transactional
	public void saveFacultyEntry(FacultyEntry facultyEntry) {
		Beans.get(FacultyEntryRepository.class).save(facultyEntry);
	}
	
	
}
