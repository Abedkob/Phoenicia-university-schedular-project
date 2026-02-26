import org.apache.poi.hpsf.Section;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import javax.swing.JOptionPane;
import java.util.regex.*;
public class CourseScheduler {
	private Map<DayOfWeek, List<LocalTime>> availableTimes1cr;
	private Map<DayOfWeek, List<LocalTime>> availableTimes2cr;
	private Map<DayOfWeek, List<LocalTime>> availableTimes3cr;
	private static List<Course> courses;
	private static List<Course> Unsched;
	private static List<Course> Sections;
	HashMap<String, Set<Course>> patternMap;
	private Map<DayOfWeek, Map<LocalTime, List<Course>>> schedule;
	private Map<String, Doctor> doctors;
	private boolean schedulingCompleted;
	public static List<Course> c;
	public static long seed;
	private Map<String, Integer> executionCounter;
	private static final Map<String, String> DAY_ABBREVIATIONS = new HashMap<>();
	public  Map<Course, Set<Doctor>> memo = new HashMap<>();
	public static List<Course> priority;

	public CourseScheduler(String courseFilePath, long seed1) {
		seed = seed1;
		doctors = new HashMap<>();
		courses = new LinkedList<>();
		Sections  = new ArrayList<>();
		patternMap = new HashMap<>();
		Unsched = new ArrayList<>();
		executionCounter = new HashMap<>();
		priority = new LinkedList<>();
		initializeSchedule();
		initializeAvailableTimes();
		this.schedule = new HashMap<>();
		readDataFromExcel(courseFilePath);
		setDoctorAvailabilityFromSheet(courseFilePath);
		setConflicts(courseFilePath);
		filterConflict(courses);
		Course.setAllCourses(courses);
		setLecturesFromOrigneCourse();
		patternRecognition();
		RegenerateTheWholeLectures();
		RegSchedule();

		DAY_ABBREVIATIONS.put("Mo", "Monday");
		DAY_ABBREVIATIONS.put("Tu", "Tuesday");
		DAY_ABBREVIATIONS.put("We", "Wednesday");
		DAY_ABBREVIATIONS.put("Th", "Thursday");
		DAY_ABBREVIATIONS.put("Fr", "Friday");

	}
	private void RegSchedule() {
		Iterator<Course> iterator = courses.iterator();
		while (iterator.hasNext()) {
			Course course = iterator.next();
			if (course.getInitialLecturesPerWeek() - course.getLectures().size() > 0 ) {
				List<Lecture> lectures = course.getLectures();
				Doctor doctor = doctors.get(course.getDoctorName());
				for (Lecture lecture : lectures) {
					doctor.addBack(lecture.getDayOfWeek(), lecture.getStartTime(), lecture.getEndTime());
				}
				course.getLectures().removeAll(lectures);
				course.setLecturesPerWeek(course.getInitialLecturesPerWeek()); 
				course.SetInitialObservationDays();
			}
		}
	}
	private void getTheNewSchedule() {
		Iterator<Course> iterator = courses.iterator();
		while (iterator.hasNext()) {
			Course course = iterator.next();
			if (course.getLecturesPerWeek() == 0) {
				List<Lecture> lectures = course.getLectures();
				Doctor doctor = doctors.get(course.getDoctorName());
				for (Lecture lecture : lectures) {
					doctor.addBack(lecture.getDayOfWeek(), lecture.getStartTime(), lecture.getEndTime());
				}
				course.getLectures().removeAll(lectures);
				course.setLecturesPerWeek(course.getInitialLecturesPerWeek()); 
				course.SetInitialObservationDays();
			}
		}
	}

	private void getUnsched() {
		Iterator<Course> iterator = courses.iterator();
		while (iterator.hasNext()) {
			Course course = iterator.next();
			if (course.getInitialLecturesPerWeek()>course.getLectures().size()) {
				Unsched.add(course);
				iterator.remove();
			}
		}
		courses.addAll(0, Unsched);
	}
	private void RegenerateTheWholeLectures() {
		HashMap<Integer, List<Course>> s = new HashMap<>();
		distributeConflictsToSections();
		DistributeSectiontoOtherCourses(); // These two methods will distribute the conflicts to the sections

		generateSchedule(); // This will generate a schedule
		getUnsched(); // This will shuffle the courses that didn't get scheduled and add them to the list of courses 
		s.put(Unsched.size(), new ArrayList<>(courses)); 
		RegSchedule();
		//getTheNewSchedule(); 
		//for (Course course : courses) {
		//	System.out.println(course.getName()+" "+course.getLecturesPerWeek()+" "+course.getObservationDays());
		//}





	}

	public void generateSchedule() {
		Collections.shuffle(courses);
		backtrack(0);
	}
	public List<Course> shuffleWithSeed(List<Course> list, long seed) {
		List<Course> shuffledList = new ArrayList<>(list);
		Random random = new Random(seed);
		for (int i = shuffledList.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			Collections.swap(shuffledList, i, j);
		}
		return shuffledList;
	}
	private void filterConflict(List<Course> courses) {
		for (Course course : courses) {
			Set<String> conflicts = new HashSet<>(course.getConflicts());
			eliminateDuplicates(conflicts, course);
		}
	}

	public static void eliminateDuplicates(Set<String> conflicts, Course course) {
		Map<String, Boolean> conflictMap = new HashMap<>();
		for (String conflict : conflicts) {

			if (conflictMap.containsKey(conflict)) {
				conflictMap.put(conflict, true);
			} else {
				conflictMap.put(conflict, false);
			}
		}

		Set<String> uniqueConflicts = new HashSet<>();
		Set<String> duplicateConflicts = new HashSet<>();

		for (Map.Entry<String, Boolean> entry : conflictMap.entrySet()) {
			if (entry.getValue()) {
				duplicateConflicts.add(entry.getKey());
			} else {
				uniqueConflicts.add(entry.getKey());
			}
		}

		conflicts.removeAll(duplicateConflicts);

		// Update the course's conflicts
		course.setConflicts(uniqueConflicts);

	}

	public List<Course> getCourses() {
		return courses;
	}

	public Map<String, Doctor> getDoctors() {
		return doctors;
	}

	public Map<DayOfWeek, Map<LocalTime, List<Course>>> getAvailableTimes() {
		return schedule;
	}

	public List<String> getConflictingCourseNames(List<Course> courses) {
		List<String> conflictingCourseNames = new ArrayList<>();
		for (Course course : courses) {
			conflictingCourseNames.add(course.getName());
		}
		return conflictingCourseNames;
	}

	void printConflictingCourses(List<Course> conflictingCourses) {
		for (Course course : conflictingCourses) {
			System.out.println("Conflicting courses for " + course.getName() + " with " + course.getDoctorName() + ":");
			Set<String> conflictNames = course.getConflicts();
			for (String conflictingCourseName : conflictNames) {
				System.out.println("- " + conflictingCourseName);
			}
		}
	}
	public Map<DayOfWeek, Map<LocalTime, List<Course>>> getSchedule() {
		return schedule;
	}

	public boolean isSchedulingCompleted() {
		return schedulingCompleted;
	}

	public boolean isCourseScheduled(Course course) {
		for (Map<LocalTime, List<Course>> dailySchedule : schedule.values()) {
			for (List<Course> coursesAtSameTime : dailySchedule.values()) {
				if (coursesAtSameTime.contains(course)) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Course> getUnscheduledCourses() {
		return Unsched;
	}

	public void setConflicts(String ConflictsPath) {
		try (FileInputStream fis = new FileInputStream(ConflictsPath);
				Workbook workbook = new XSSFWorkbook(fis)) {
			Sheet sheet = workbook.getSheetAt(2);
			for (Row row : sheet) {
				Set<String> conflictCodes = new HashSet<>(); 
				for (Cell cell : row) {
					if (cell.getCellType() == CellType.STRING) {
						conflictCodes.add(cell.getStringCellValue().trim());
					}
				}

				Set<String> filteredConflicts = new HashSet<>(conflictCodes);

				for (String code : filteredConflicts) {
					Course course = findCourseByCode(code);
					if (course != null) {
						Set<String> existingConflicts = new HashSet<>(course.getConflicts());
						Set<String> newConflicts = new HashSet<>(filteredConflicts);
						newConflicts.remove(code);
						Set<String> updatedConflicts = updateCourseConflicts(existingConflicts, newConflicts);

						course.setConflicts(updatedConflicts);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Set<String> updateCourseConflicts(Set<String> existingConflicts, Set<String> newConflicts) {
		Set<String> combinedConflicts = new HashSet<>(existingConflicts);
		for (String newConflict : newConflicts) {
			if (!existingConflicts.contains(newConflict)) {
				combinedConflicts.add(newConflict);
			}
		}

		return combinedConflicts;
	}

	private Course findCourseByCode(String code) {
		for (Course course : courses) {
			if (course.getName().equalsIgnoreCase(code)) {
				return course;
			}
		}
		return null;
	}


	public void setDoctorAvailabilityFromSheet(String excelFilePath) {
		Set<String> doctorsWithAvailability = new HashSet<>();
		Set<String> doctorsFromCourses = new HashSet<>();

		try (FileInputStream fis = new FileInputStream(excelFilePath);
				Workbook workbook = new XSSFWorkbook(fis)) {
			Sheet availabilitySheet = workbook.getSheet("Availability Sheet");
			for (Row row : availabilitySheet) {
				String doctorName = row.getCell(0).getStringCellValue().trim();
				if (doctorName.isEmpty()) {
					JOptionPane.showMessageDialog(null, "Doctor name is missing at row " + (row.getRowNum() + 1),
							"Error",
							JOptionPane.ERROR_MESSAGE);
					continue;
				}

				Doctor doctor = doctors.computeIfAbsent(doctorName, Doctor::new);
				boolean hasAvailability = false;

				for (int i = 1; i <= DayOfWeek.values().length; i++) {
					Cell availabilityCell = row.getCell(i);
					if (availabilityCell == null) {
						continue;
					}

					String availability = availabilityCell.getStringCellValue().trim();
					if (!availability.isEmpty()) {
						hasAvailability = true;
						String[] parts = availability.split("\\s*,\\s*");
						DayOfWeek dayOfWeek = DayOfWeek.of(i);

						for (String part : parts) {
							String[] timings = part.split("-");
							if (timings.length != 2 || !timings[0].matches("\\d{2}:\\d{2}")
									|| !timings[1].matches("\\d{2}:\\d{2}")) {
								JOptionPane.showMessageDialog(null,
										"Invalid time slot format at row " + (row.getRowNum() + 1) + ": " + part,
										"Error",
										JOptionPane.ERROR_MESSAGE);
								continue;
							}

							try {
								LocalTime startTime = parseTime(timings[0].trim());
								LocalTime endTime = parseTime(timings[1].trim());
								doctor.addAvailability(dayOfWeek, startTime, endTime);
							} catch (DateTimeParseException e) {
								JOptionPane.showMessageDialog(null,
										"Error parsing time slot at row " + (row.getRowNum() + 1) + ": "
												+ e.getMessage(),
												"Error", JOptionPane.ERROR_MESSAGE);
							}
						}
					}
				}

				if (!hasAvailability) {
					JOptionPane.showMessageDialog(null,
							"Doctor " + doctorName + " has no availability set at row " + (row.getRowNum() + 1),
							"Error",
							JOptionPane.ERROR_MESSAGE);
				} else {
					doctorsWithAvailability.add(doctorName);
				}
			}
			Sheet coursesSheet = workbook.getSheet("Course Sheet");
			for (Row row : coursesSheet) {
				String doctorName = row.getCell(1).getStringCellValue().trim();
				if (!doctorName.isEmpty()) {
					doctorsFromCourses.add(doctorName);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		checkDoctorCoverage(doctorsWithAvailability, doctorsFromCourses);
	}

	private void checkDoctorCoverage(Set<String> doctorsWithAvailability, Set<String> doctorsFromCourses) {
		for (String doctorName : doctorsFromCourses) {
			if (!doctorsWithAvailability.contains(doctorName)) {
				JOptionPane.showMessageDialog(null,
						"Doctor " + doctorName + " assigned to teach a course is not listed in the availability sheet.",
						"Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private LocalTime parseTime(String timeStr) {
		if (timeStr.matches("^\\d{1,2}:\\d{2}$")) {
			return LocalTime.parse(timeStr);
		} else if (timeStr.matches("^\\d{1,2}:\\d{1,2}$")) {
			String[] parts = timeStr.split(":");
			return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
		} else {
			throw new IllegalArgumentException("Invalid time format: " + timeStr);
		}
	}

	void readDataFromExcel(String filename) {
		try (FileInputStream fis = new FileInputStream(filename); Workbook workbook = new XSSFWorkbook(fis)) {
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row : sheet) {
				int rowIndex = row.getRowNum() + 1;

				String name = getStringValue(row.getCell(0));
				if (name == null || name.isEmpty() || !name.matches("[A-Za-z0-9]+")) {
					JOptionPane.showMessageDialog(null, "Empty or Invalid course name at row " + rowIndex + ": " + name,
							"Error", JOptionPane.ERROR_MESSAGE);
					continue;
				}

				String doctorName = getStringValue(row.getCell(1));
				if (doctorName != null && doctorName.startsWith("Dr.")) {
					doctorName = doctorName.substring(3).trim();
				}

				if (doctorName == null || doctorName.isEmpty() || !doctorName.matches("[A-Za-z ]+")) {
					JOptionPane.showMessageDialog(null,
							"Empty or Invalid doctor name at row " + rowIndex + ": " + doctorName, "Error",
							JOptionPane.ERROR_MESSAGE);
				}

				int credits = getNumericValue(row.getCell(2));
				if (credits != 1 && credits != 2 && credits != 3) {
					JOptionPane.showMessageDialog(null, "Invalid credit value at row " + rowIndex + ": " + credits,
							"Error", JOptionPane.ERROR_MESSAGE);
				}

				int lecturesPerWeek = getNumericValue(row.getCell(3));
				if (lecturesPerWeek <= 0) {
					JOptionPane.showMessageDialog(null,
							"Invalid lecture count at row " + rowIndex + ": " + lecturesPerWeek, "Error",
							JOptionPane.ERROR_MESSAGE);
				}

				double periodPerWeek = getNumericValue(row.getCell(4));
				String observationDaysCell = getStringValue(row.getCell(5));
				List<DayOfWeek> observationDaysList = parseShortcuts(observationDaysCell);
				if (observationDaysList.isEmpty()) {
					JOptionPane.showMessageDialog(null,
							"Empty or Invalid observation days at row " + rowIndex + ": " + observationDaysCell,
							"Error", JOptionPane.ERROR_MESSAGE);
				}		
				String Priority = getStringValue(row.getCell(6));
				boolean prio = false;
				if (Priority != null && Priority.equalsIgnoreCase("p")) {
					prio = true;
				}

				Course course = new Course(name, doctorName, credits, lecturesPerWeek, periodPerWeek,
						observationDaysList,prio);
				courses.add(course);
				
				course.SetInitialObservationDayss(observationDaysList);
				if(prio) {
					priority.add(course);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void printCourses() {
		System.out.println("The courses are :");
		for (Course course : courses) {
			System.out.print(course.getName()+" ");
		}
		System.out.println( );
	}
	public boolean allPriorityAreSchedulued(){
		boolean flag = true;
		for (Course course : priority) {
			if(course.getLecturesPerWeek() != 0 && course.getLectures().size() != course.getInitialLecturesPerWeek()) {
				return false;
			}
			
		}
		return flag;
		
	}

	private List<DayOfWeek> parseShortcuts(String shortcuts) {
		List<DayOfWeek> daysOfWeek = new ArrayList<>();
		if (shortcuts != null && !shortcuts.isEmpty()) {
			String[] pairs = shortcuts.split("-");
			for (int i = 0; i < pairs.length; i += 2) {
				int count = Integer.parseInt(pairs[i]);
				DayOfWeek dayOfWeek = getDayOfWeek(pairs[i + 1]);
				if (dayOfWeek != null) {
					for (int j = 0; j < count; j++) {
						daysOfWeek.add(dayOfWeek);
					}
				}
			}
		}
		return daysOfWeek;
	}

	private DayOfWeek getDayOfWeek(String abbreviation) {
		switch (abbreviation.toUpperCase()) {
		case "MON":
		case "Mon":
		case "mon":
		case "MOn":
			return DayOfWeek.MONDAY;
		case "TUE":
		case "Tue":
		case "tue":
		case "TUe":
			return DayOfWeek.TUESDAY;
		case "WED":
		case "Wed":
		case "wed":
		case "WEd":
			return DayOfWeek.WEDNESDAY;
		case "THU":
		case "Thu":
		case "thu":
		case "THu":
			return DayOfWeek.THURSDAY;
		case "FRI":
		case "Fri":
		case "fri":
		case "FRi":
			return DayOfWeek.FRIDAY;
		default:
			return null;
		}
	}

	private int getNumericValue(Cell cell) {
		if (cell == null) {
			return 0;
		} else if (cell.getCellType() == CellType.NUMERIC) {
			return (int) cell.getNumericCellValue();
		} else {
			return 0;
		}
	}

	private String getStringValue(Cell cell) {
		if (cell == null)
			return "";
		return cell.getStringCellValue();
	}

	private void initializeAvailableTimes() {
		availableTimes1cr = new HashMap<>();
		availableTimes2cr = new HashMap<>();
		availableTimes3cr = new HashMap<>();
		List<LocalTime> allowedTimes1 = Arrays.asList(
				LocalTime.of(8, 0),
				LocalTime.of(8, 30),
				LocalTime.of(9, 0),
				LocalTime.of(9, 30),
				LocalTime.of(10, 0),
				LocalTime.of(10, 30),
				LocalTime.of(11, 0),
				LocalTime.of(13, 0),
				LocalTime.of(14, 0),
				LocalTime.of(14, 30),
				LocalTime.of(15, 0),
				LocalTime.of(15, 30),
				LocalTime.of(16, 0));
		List<LocalTime> allowedTimes3cr = Arrays.asList(
				LocalTime.of(8, 0),
				LocalTime.of(9, 30),
				LocalTime.of(11, 0),
				LocalTime.of(13, 0),
				LocalTime.of(14, 30),
				LocalTime.of(16, 0));

		List<LocalTime> allowedTimes2cr = Arrays.asList(
				LocalTime.of(8, 0),
				LocalTime.of(10, 15),
				LocalTime.of(13, 0),
				LocalTime.of(15, 15));

		for (DayOfWeek day : DayOfWeek.values()) {
			List<LocalTime> times = new ArrayList<>();
			for (LocalTime time : allowedTimes1) {
				if (!((day == DayOfWeek.MONDAY || day == DayOfWeek.TUESDAY || day == DayOfWeek.WEDNESDAY ||
						day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY) &&
						(time.equals(LocalTime.of(12, 16)) || time.equals(LocalTime.of(12, 59))))) {
					times.add(time);
				}
			}
			availableTimes1cr.put(day, times);
		}
		for (DayOfWeek day : DayOfWeek.values()) {
			List<LocalTime> times = new ArrayList<>();
			for (LocalTime time : allowedTimes2cr) {
				if (!((day == DayOfWeek.MONDAY || day == DayOfWeek.TUESDAY || day == DayOfWeek.WEDNESDAY ||
						day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY) &&
						(time.equals(LocalTime.of(12, 16)) || time.equals(LocalTime.of(12, 59))))) {
					times.add(time);
				}
			}
			availableTimes2cr.put(day, times);
		}
		for (DayOfWeek day : DayOfWeek.values()) {
			List<LocalTime> times = new ArrayList<>();
			for (LocalTime time : allowedTimes3cr) {
				if (!((day == DayOfWeek.MONDAY || day == DayOfWeek.TUESDAY || day == DayOfWeek.WEDNESDAY ||
						day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY) &&
						(time.equals(LocalTime.of(12, 16)) || time.equals(LocalTime.of(12, 59))))) {
					times.add(time);
				}
			}
			availableTimes3cr.put(day, times);
		}

	}

	private void initializeSchedule() {
		schedule = new HashMap<>();
	}
	public static boolean matchesPattern(String str) {
		String regex = "^[A-Z]{4}\\d{3}[A-Z]\\d$";
		Pattern compiledPattern = Pattern.compile(regex);
		Matcher matcher = compiledPattern.matcher(str);
		return matcher.matches();
	}

	private void setLecturesFromOrigneCourse() {
		for (Course course : courses) {
			String courseCode = course.getName();
			if (matchesPattern(courseCode)) {
				Sections.add(course);

			}
		}



	}


	private void patternRecognition() {
		for (Course course : Sections) {
			String courseCode = course.getName();
			String patternKey = courseCode.substring(0, 7);
			patternMap.computeIfAbsent(patternKey, k -> new HashSet<>()).add(course);
		}
	}
	private void distributeConflictsToSections() {
		for (Set<Course> courseSet : patternMap.values()) {
			for (Course course : courseSet) {
				if (!course.getConflicts().isEmpty()) {
					for (Course otherCourse : courseSet) {
						if (!otherCourse.getName().equals(course.getName())) {
							otherCourse.setConflicts(course.getConflicts());

						}
					}
				}
			}
			for (Course course : courseSet) {
				if (course.getConflicts().isEmpty()) {
					for (Course otherCourse : courseSet) {
						if (!otherCourse.equals(course) && !otherCourse.getConflicts().isEmpty()) {
							course.setConflicts(otherCourse.getConflicts());
							break;
						}
					}
				}
			}
		}
	}
	private void DistributeSectiontoOtherCourses() {
		for (Map.Entry<String, Set<Course>> entry : patternMap.entrySet()) {
			Set<Course> courseSet = entry.getValue();
			for (Course course : courseSet) {
				Set<String> conflicts = course.getConflicts();
				for (String conflictCode : conflicts) {
					Course conflictCourse = findCourseByCode(conflictCode);

					if (conflictCourse != null) {
						// Propagate conflict to other sections within the same courseSet
						for (Course relatedCourse : courseSet) {
							if (!relatedCourse.getName().equals(course.getName())) {
								Set<String> relatedConflicts = conflictCourse.getConflicts();
								relatedConflicts.add(relatedCourse.getName());
								conflictCourse.setConflicts(relatedConflicts);
							}
						}

						// Propagate conflict to corresponding sections in conflictCourse's set
						String conflictCourseName = conflictCourse.getName();
						String patternKey = conflictCourseName.substring(0, 7);
						Set<Course> correspondingSet = patternMap.get(patternKey);
						if (correspondingSet != null) {
							for (Course correspondingCourse : correspondingSet) {
								if (!correspondingCourse.getName().equals(conflictCourseName)) {
									Set<String> correspondingConflicts = correspondingCourse.getConflicts();
									correspondingConflicts.add(course.getName());
									correspondingCourse.setConflicts(correspondingConflicts);
								}
							}
						}
					}
				}
			}
		}

	}
	private void RemoveTheOtherSection(Course course) {
		if (course == null) {
			System.out.println("Course is null");
			return;
		}

		String courseCode = course.getName();
		if (courseCode.length() < 7) {
			System.out.println("Course code is too short: " + courseCode);
			return;
		}
		String patternKey = courseCode.substring(0, 7);

		Set<Course> courseSet = patternMap.get(patternKey);
		if (courseSet == null || courseSet.size() < 2) {
			System.out.println("Course set is null or has less than 2 courses");
			return;
		}

		Course counterpart = null;
		for (Course c : courseSet) {
			if (!c.getName().equals(courseCode)) {
				counterpart = c;
				break;
			}
		}

		if (counterpart == null) {
			System.out.println("No counterpart found for course: " + courseCode);
			return;
		}

		Set<String> counterpartConflicts = counterpart.getConflicts();
		for (String conflictCode : counterpartConflicts) {
			Course conflictCourse = findCourseByCode(conflictCode);
			if (conflictCourse != null) {
				conflictCourse.getConflicts().remove(counterpart.getName());
			}
		}

		counterpartConflicts.clear(); // Clear all conflicts for the counterpart course
		counterpart.setConflicts(counterpartConflicts);
	}
	private void backtrack(int courseIndex) {
		if (courseIndex == courses.size()) {
			schedulingCompleted = true;
			return;
		}

		Course currentCourse = courses.get(courseIndex);

		if (currentCourse.getLecturesPerWeek() == 0) {
			backtrack(courseIndex + 1);
			return;
		}

		Doctor doctor = doctors.get(currentCourse.getDoctorName());

		if (doctor != null) {
			List<DayOfWeek> observationDays = new ArrayList<>(currentCourse.getObservationDays());

			for (DayOfWeek dayOfWeek : observationDays) {
		
				List<LocalTime> allowedTimes;
				if (currentCourse.getCredits() == 1) {
					allowedTimes = availableTimes1cr.get(dayOfWeek);
				} else if (currentCourse.getCredits() == 2) {
					allowedTimes = availableTimes2cr.get(dayOfWeek);
				} else {
					allowedTimes = availableTimes3cr.get(dayOfWeek);
				}

				int courseCredits = currentCourse.getCredits();
				int duration = (courseCredits == 2) ? 120 : (courseCredits == 3) ? 75 : 50;

				for (LocalTime startTime : allowedTimes) {
					LocalTime endTime = startTime.plusMinutes(duration);					
					if (isSlotAvailable(currentCourse, doctor, dayOfWeek, startTime, endTime, courses)
							&& currentCourse.getLecturesPerWeek() > 0) {
						scheduleCourse(currentCourse, dayOfWeek, startTime, endTime, doctor);
						if(currentCourse.getLecturesPerWeek() == 0) {
							CheckIfItExistsInHashMap(currentCourse);
						}
						backtrack(courseIndex);

					}

					if (currentCourse.getLecturesPerWeek() == currentCourse.getInitialLecturesPerWeek()) {
						backtrack(courseIndex + 1);
					}

				}
			}
		}
		if(currentCourse.getInitialLecturesPerWeek() - currentCourse.getLectures().size() > 0 && !currentCourse.getLectures().isEmpty()) {

			List<Lecture> lectures = currentCourse.getLectures();
			for (Lecture lecture : lectures) {
				doctor.addBack(lecture.getDayOfWeek(), lecture.getStartTime(), lecture.getEndTime());
			}
			currentCourse.getLectures().removeAll(lectures);
			currentCourse.setLecturesPerWeek(currentCourse.getInitialLecturesPerWeek());
			currentCourse.SetInitialObservationDays();

		}
	}
	private List<LocalTime> getAvailableTimes(Course currentCourse, DayOfWeek dayOfWeek) {
		switch (currentCourse.getCredits()) {
		case 1:
			return availableTimes1cr.get(dayOfWeek);
		case 2:
			return availableTimes2cr.get(dayOfWeek);
		case 3:
			return availableTimes3cr.get(dayOfWeek);
		default:
			return new ArrayList<>();
		}
	}

	private int getCourseDuration(int courseCredits) {
		return (courseCredits == 2) ? 120 : (courseCredits == 3) ? 75 : 50;
	}

	private void rollbackSchedule(Course currentCourse, Doctor doctor, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		currentCourse.removeLastLecture();
		currentCourse.setLecturesPerWeek(currentCourse.getLecturesPerWeek() + 1);
		doctor.addBack(dayOfWeek, startTime, endTime);

	}

	private void rollbackAllLectures(Course currentCourse, Doctor doctor) {
		List<Lecture> lectures = new ArrayList<>(currentCourse.getLectures());
		for (Lecture lecture : lectures) {
			rollbackSchedule(currentCourse, doctor, lecture.getDayOfWeek(), lecture.getStartTime(), lecture.getEndTime());
		}
		currentCourse.setLecturesPerWeek(currentCourse.getInitialLecturesPerWeek());
		currentCourse.SetInitialObservationDays();
	}
	private void redoScheduling() {
		Iterator<Course> iterator = courses.iterator();
		while (iterator.hasNext()) {
			Course course = iterator.next();
			if (course.getLecturesPerWeek() > 0) {
				List<Lecture> lectures = course.getLectures();
				Doctor doctor = doctors.get(course.getDoctorName());
				for (Lecture lecture : lectures) {
					doctor.addBack(lecture.getDayOfWeek(), lecture.getStartTime(), lecture.getEndTime());
				}
				course.getLectures().removeAll(lectures);
				course.setLecturesPerWeek(course.getInitialLecturesPerWeek()); 
			}


		}
	}



	private void CheckIfItExistsInHashMap(Course currentCourse) {
		String courseName = currentCourse.getName();
		String patternKey = courseName.substring(0, 7);

		// Update counter
		executionCounter.putIfAbsent(patternKey, 0);
		int currentCount = executionCounter.get(patternKey);

		if (currentCount < 1) {
			if (patternMap.containsKey(patternKey)) {
				Set<Course> courseSet = patternMap.get(patternKey);

				for (Course course : courseSet) {
					if (course.isSchedulued() && course.getLecturesPerWeek() == 0 && currentCourse.getName().equals(course.getName())) {
						RemoveTheOtherSection(course);
						RemoveTheConflictForTheValue(course);
						executionCounter.put(patternKey, currentCount + 1);
						break;
					}
				}
			}
		}
	}

	private void RemoveTheConflictForTheValue(Course course) {
		String courseCode = course.getName();
		String patternKey = courseCode.substring(0, 7);
		Set<Course> courseSet = patternMap.get(patternKey);

		if (courseSet == null || courseSet.size() < 2) {
			return;
		}

		for (Course c : courseSet) {
			if (!c.getName().equals(courseCode)) {
				c.getConflicts().removeAll(c.getConflicts());
			}
		}
	}
	private void unscheduleCourse(Course course, Map<Course, Map<DayOfWeek, Map<LocalTime, LocalTime>>> memo) {
		if (!memo.containsKey(course)) {
			return;
		}

		Map<DayOfWeek, Map<LocalTime, LocalTime>> courseMemo = memo.get(course);
		for (DayOfWeek day : courseMemo.keySet()) {
			Map<LocalTime, LocalTime> dayMemo = courseMemo.get(day);
			for (LocalTime startTime : dayMemo.keySet()) {
				LocalTime endTime = dayMemo.get(startTime);
				Doctor doctor = doctors.get(course.getDoctorName());
				undoScheduling(course, day, startTime, doctor);
			}
		}

		memo.remove(course);

	}


	private void undoScheduling(Course course, DayOfWeek dayOfWeek, LocalTime startTime, Doctor doctor) {
		int courseCredits = course.getCredits();
		int duration = (courseCredits == 2) ? 120 : (courseCredits == 3) ? 75 : 50;
		LocalTime endTime = startTime.plusMinutes(duration);
		Map<LocalTime, List<Course>> dailySchedule = schedule.computeIfAbsent(dayOfWeek, k -> new HashMap<>());
		List<Course> coursesAtSameTime = dailySchedule.computeIfAbsent(startTime, k -> new ArrayList<>());
		coursesAtSameTime.remove(course);
		dailySchedule.remove(startTime, coursesAtSameTime);
		course.UndoLecture(dayOfWeek, startTime, endTime);
		doctor.addBack(dayOfWeek, startTime, endTime);
		course.setLecturesPerWeek(course.getInitialLecturesPerWeek());
	}

	private boolean isSlotAvailable(Course currentCourse, Doctor doctor, DayOfWeek dayOfWeek, LocalTime startTime,
			LocalTime endTime, List<Course> courses) {
		boolean isAvailable = doctor.isAvailable(dayOfWeek, startTime, endTime);
		boolean conflict = currentCourse.conflictingWithCourses(dayOfWeek, startTime, endTime, courses);
		return isAvailable && !conflict;
	}

	private void scheduleCourse(Course currentCourse, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
			Doctor doctor) {
		Map<LocalTime, List<Course>> dailySchedule = schedule.computeIfAbsent(dayOfWeek, k -> new HashMap<>());
		List<Course> coursesAtSameTime = dailySchedule.computeIfAbsent(startTime, k -> new ArrayList<>());
		coursesAtSameTime.add(currentCourse);
		dailySchedule.put(startTime, coursesAtSameTime);
		currentCourse.scheduleLecture(dayOfWeek, startTime, endTime);	
		currentCourse.setLecturesPerWeek(currentCourse.getLecturesPerWeek()-1);
		doctor.addScheduledTime(dayOfWeek, startTime, endTime);
	}

	public static void main(String[] args) {
	}


	private void displayScheduledAndUnscheduledCourses() {
		System.out.println("Scheduled Courses:");
		boolean unscheduledCoursesExist = false;

		for (DayOfWeek day : DayOfWeek.values()) {
			if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
				System.out.println(day + ":");

				Map<LocalTime, List<Course>> dailySchedule = schedule.getOrDefault(day, Collections.emptyMap());
				List<LocalTime> timeSlots = Arrays.asList(
						LocalTime.of(8, 0), LocalTime.of(9, 5), LocalTime.of(9, 30),
						LocalTime.of(10, 15), LocalTime.of(10, 30),
						LocalTime.of(11, 0), LocalTime.of(11, 20),
						LocalTime.of(13, 0),
						LocalTime.of(14, 5), LocalTime.of(14, 30),
						LocalTime.of(15, 10), LocalTime.of(15, 15),
						LocalTime.of(16, 0),
						LocalTime.of(16, 5));

				for (LocalTime time : timeSlots) {
					List<Course> coursesAtSameTime = dailySchedule.getOrDefault(time, new ArrayList<>());
					for (Course course : coursesAtSameTime) {
						if (course.getLecturesPerWeek() == 0) {
							LocalTime endTime = time.plusMinutes((long) course.getDurationMinutes());
							System.out.println(
									"- " + course.getName() + " from " + time + " to " + endTime + " with "
											+ course.getDoctorName());
						} else {
							unscheduledCoursesExist = true;
						}
					}
				}
			}
		}

		if (!unscheduledCoursesExist) {
			System.out.println("All courses are scheduled.");
		}

		System.out.println("\nUnscheduled Courses:");
		for (Course course : courses) {
			if (course.getLecturesPerWeek() > 0) {
				System.out.println("- " + course.getName() + " with " + course.getDoctorName());				
				for (Map<LocalTime, List<Course>> dailySchedule : schedule.values()) {
					for (List<Course> coursesAtSameTime : dailySchedule.values()) {
						coursesAtSameTime.remove(course);
					}
				}
				unscheduledCoursesExist = true;
			}
		}

		if (!unscheduledCoursesExist) {
			System.out.println("All courses are scheduled.");
		}

	}
}