package com.axelor.apps.admission.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axelor.apps.admission.db.AdmissionEntry;
import com.axelor.apps.admission.db.AdmissionProcess;
import com.axelor.apps.admission.db.College;
import com.axelor.apps.admission.db.CollegeEntry;
import com.axelor.apps.admission.db.FacultyEntry;
import com.axelor.apps.admission.db.repo.AdmissionEntryRepository;
import com.axelor.apps.admission.db.repo.CollegeRepository;
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

		Map<Long, Map<Long, Integer>> collegeMap = initSeatTracker(admissionEntries);

		for (AdmissionEntry entry : admissionEntries) {
			List<CollegeEntry> collegeEntries = getSortedCollegeEntryList(entry.getCollegeEntryList());

			for (CollegeEntry collegeEntry : collegeEntries) {
				College college = collegeEntry.getCollege();
				for (FacultyEntry facultyEntry : college.getFacultyEntryList()) {
					if (entry.getFaculty().getId().equals(facultyEntry.getFaculty().getId())
							&& collegeMap.get(college.getId()).get(facultyEntry.getFaculty().getId()) > 0
							&& !isCollegeSelected) {
						
						entry.setCollegeSelected(college);
						isCollegeSelected = true;
						Map<Long, Integer> facultyMap = collegeMap.get(college.getId());
						int cnt = facultyMap.get(facultyEntry.getFaculty().getId());
						facultyMap.put(facultyEntry.getFaculty().getId(), --cnt);
						collegeMap.put(college.getId(), facultyMap);

						entry.setValidationDate(LocalDate.now());
						entry.setStatusSelect(AdmissionEntryRepository.STATUS_ADMITTED);
						
					} else if (entry.getFaculty().getId().equals(facultyEntry.getFaculty().getId())
							&& collegeMap.get(college.getId()).get(facultyEntry.getFaculty().getId()) <= 0)
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

	public List<CollegeEntry> getSortedCollegeEntryList(List<CollegeEntry> collegeEntries) {
		Collections.sort((List<CollegeEntry>) collegeEntries, new Comparator<CollegeEntry>() {
			@Override
			public int compare(CollegeEntry o1, CollegeEntry o2) {
				return o1.getSequence().compareTo(o2.getSequence());
			}
		});
		return collegeEntries;
	}

	public Map<Long, Map<Long, Integer>> initSeatTracker(List<AdmissionEntry> admissionEntries) {
		Map<Long, Map<Long, Integer>> collegeMap = new HashMap<>();

		List<College> colleges = Beans.get(CollegeRepository.class).all().fetch();

		for (College college : colleges) {
			Map<Long, Integer> facultyMap = new HashMap<>();
			for (FacultyEntry facultyEntry : college.getFacultyEntryList()) {
				facultyMap.put(facultyEntry.getFaculty().getId(), facultyEntry.getSeats());
			}
			collegeMap.put(college.getId(), facultyMap);
		}

		return collegeMap;
	}
}
