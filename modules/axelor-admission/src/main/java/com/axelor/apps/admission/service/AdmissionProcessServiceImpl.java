package com.axelor.apps.admission.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

	@Override
	public void setAllAdmissions(AdmissionProcess admissionProcess) {

		boolean isCollegeSelected = false;

		List<AdmissionEntry> admissionEntries = Beans.get(AdmissionEntryRepository.class).all().filter(
				"self.registrationDate > :fromDate AND self.registrationDate <= :toDate AND self.statusSelect = :statusComfirmed")
				.order("-merit").order("registrationDate").bind("fromDate", admissionProcess.getFromDate())
				.bind("statusComfirmed", AdmissionEntryRepository.STATUS_CONFIRMED)
				.bind("toDate", admissionProcess.getToDate()).fetch();

		for (AdmissionEntry entry : admissionEntries) {
			List<CollegeEntry> collegeEntries = getSortedCollegeEntryList(entry.getCollegeEntryList());

			for (CollegeEntry collegeEntry : collegeEntries) {
				College college = collegeEntry.getCollege();
				for (FacultyEntry facultyEntry : college.getFacultyEntryList()) {
					if (entry.getFaculty().getId().equals(facultyEntry.getFaculty().getId())
							&& facultyEntry.getSeats() > 0) {
						if (!isCollegeSelected) {
							entry.setCollegeSelected(college);
							isCollegeSelected = true;
							int cnt = facultyEntry.getSeats();
							facultyEntry.setSeats(--cnt);
							this.saveFacultyEntry(facultyEntry);
						}
						entry.setValidationDate(LocalDate.now());
						entry.setStatusSelect(AdmissionEntryRepository.STATUS_ADMITTED);
					} else if (entry.getFaculty().getId().equals(facultyEntry.getFaculty().getId())
							&& facultyEntry.getSeats() <= 0)
						entry.setStatusSelect(AdmissionEntryRepository.STATUS_CANCELLED);
				}
			}
			this.saveAdmissionEntry(entry);
			isCollegeSelected = false;
		}
		
	}

	@Transactional
	public void saveAdmissionEntry(AdmissionEntry admissionEntry) {
		Beans.get(AdmissionEntryRepository.class).save(admissionEntry);
	}

	@Transactional
	public void saveFacultyEntry(FacultyEntry facultyEntry) {
		Beans.get(FacultyEntryRepository.class).save(facultyEntry);
	}

	public List<CollegeEntry> getSortedCollegeEntryList(List<CollegeEntry> collegeEntries) {
		Collections.sort((List<CollegeEntry>) collegeEntries, new Comparator<CollegeEntry>() {
			@Override
			public int compare(CollegeEntry o1, CollegeEntry o2) {
				return o1.getSequence().compareTo(o2.getSequence());
			}
		});
		return collegeEntries;
	}
}
