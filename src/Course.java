
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Course {
	private String name;
	private String doctorName;
	private int credits;
	private int lecturesPerWeek;
	private double periodPerWeek;
	private Set<String> conflicts;
	private int initialLecturesPerWeek;
	private List<DayOfWeek> observationDays;
	private List<DayOfWeek> IntialobservationDays;
	private List<Lecture> Lectures;
	private static List<Course> Allcourses;
	private boolean priority;

	public Course(String name, String doctorName, int credits, int lecturesPerWeek,
			double periodPerWeek, List<DayOfWeek> observationDayss,boolean prio) {
		this.name = name;
		this.doctorName = doctorName;
		this.credits = credits;
		this.lecturesPerWeek = lecturesPerWeek;
		initialLecturesPerWeek = lecturesPerWeek;
		this.periodPerWeek = periodPerWeek;
		this.conflicts = new HashSet<>();
		this.priority = prio;
		this.observationDays = observationDayss;
		this.IntialobservationDays = observationDayss;
		this.Lectures = new ArrayList<>();

		if (Allcourses == null) {
			Allcourses = new ArrayList<>();
		}
		Allcourses.add(this);

	}
	public boolean isPrioritize() {
		return priority;
	}
	public void SetInitialObservationDayss(List<DayOfWeek> d) {
		IntialobservationDays = new ArrayList<DayOfWeek>(d);
	}
	public void SetInitialObservationDays() {
		setObservationDays(IntialobservationDays);
	}
	public void getInitialObservationDays() {
		System.out.println(IntialobservationDays.toString());
	}
	public static List<Course> getAllCourses() {
		return Allcourses;
	}

	public static void setAllCourses(List<Course> courses) {
		Allcourses = courses;
	}

	public List<Lecture> getLectures() {
		return Lectures;
	}

	public void setLectures(List<Lecture> lectures) {
		Lectures = lectures;
	}

	public int getInitialLecturesPerWeek() {
		return initialLecturesPerWeek;
	}

	public void setInitialLecturesPerWeek(int initialLecturesPerWeek) {
		this.initialLecturesPerWeek = initialLecturesPerWeek;
	}
	public Course findCourseByName(String courseName) {
		for (Course course : Allcourses) {
			if (course.getName().equalsIgnoreCase(courseName)) {
				return course;
			}
		}
		return null;
	}

	public void undoConflictLecture(Course course) {
		Set<String> conflicts = course.getConflicts();
		List<Lecture> courseLectures = course.getLectures();

		for (String conflictCourseName : conflicts) {
			Course conflictCourse = findCourseByName(conflictCourseName);

			if (conflictCourse != null) {
				Iterator<Lecture> iterator = conflictCourse.getLectures().iterator();
				while (iterator.hasNext()) {
					Lecture lecture = iterator.next();
					conflictCourse.setLecturesPerWeek(conflictCourse.getLecturesPerWeek()+1);
					iterator.remove();
				}
			} else {
			}
		}
	}



	public List<DayOfWeek> getObservationDays() {
		return observationDays;
	}

	public List<DayOfWeek> setObservationDays(List<DayOfWeek> list) {
		return this.observationDays = list;
	}

	public void setPeriodPerWeek(double periodPerWeek) {
		this.periodPerWeek = periodPerWeek;
	}

	public void incrementLecturesPerWeek() {
		this.lecturesPerWeek++;
	}

	public void resetLecturesPerWeek() {
		this.lecturesPerWeek = this.initialLecturesPerWeek;
	}
	public boolean isSchedulued() {
		if(this.lecturesPerWeek == 0) {
			return true;
		}
		return false;
	}

	public void scheduleLecture(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		if (lecturesPerWeek > 0) {

			if (observationDays.contains(dayOfWeek)) {
				for (Lecture lecture : Lectures) {
					if (lecture.getDayOfWeek() == dayOfWeek &&
							((lecture.getStartTime().isBefore(endTime) && lecture.getEndTime().isAfter(startTime)) ||
									(lecture.getStartTime().equals(startTime)
											&& lecture.getEndTime().equals(endTime)))) {
						return;
					}
				}
				Lecture newLecture = new Lecture(dayOfWeek, startTime, endTime);
				Lectures.add(newLecture);

				observationDays.remove(dayOfWeek); 

			} else {

			}
		} else {

		}
	}


	public String getName() {
		return name;
	}

	public String getDoctorName() {
		return doctorName;
	}

	public int getCredits() {
		return credits;
	}

	public int getLecturesPerWeek() {
		return lecturesPerWeek;
	}

	public double getDurationMinutes() {
		return periodPerWeek;
	}

	public Set<String> getConflicts() {
		return conflicts;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDoctorName(String doctorName) {
		this.doctorName = doctorName;
	}

	public void setCredits(int credits) {
		this.credits = credits;
	}

	public void setLecturesPerWeek(int lecturesPerWeek) {
		this.lecturesPerWeek = lecturesPerWeek;
	}

	public void setPeriodPerWeek(int periodPerWeek) {
		this.periodPerWeek = periodPerWeek;
	}

	public void setConflicts(Set<String> updatedConflicts) {
		this.conflicts.addAll(updatedConflicts);
	}

	public void decrementLecturesPerWeek() {
		this.lecturesPerWeek--;
	}

	public void displayLectures() {
		for (Lecture lecture : Lectures) {
			System.out.println("Course: " + name);
			System.out.println("Lecture Day: " +
					lecture.getDayOfWeek());
			System.out.println("Start Time: " +
					lecture.getStartTime());
			System.out.println("End Time: " +
					lecture.getEndTime());
			System.out.println("-------------------------");
		}
	}

	public void UndoLecture(DayOfWeek day, LocalTime start, LocalTime end) {
		for (Lecture lecture : Lectures) {
			if (lecture.getDayOfWeek() == day &&
					lecture.getStartTime().equals(start) &&
					lecture.getEndTime().equals(end)) {
				Lectures.remove(lecture);
				System.out.println("Lecture undone for " + day + " from " + start + " to " + end);
				return;
			}
		}
		observationDays.add(day);


	}

	public boolean conflictingWithCourses(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,List<Course> courses) {
		for (String conflictCourseName : conflicts) {
			for (Course conflictCourse : courses) {
				if (conflictCourse.getName().equalsIgnoreCase(conflictCourseName)) {
					List<Lecture> conflictLectures = conflictCourse.getLectures();
					if (conflictLectures != null) {
						for (Lecture lecture : conflictLectures) {
							if (lecture.getDayOfWeek() == dayOfWeek &&
									((lecture.getStartTime().isBefore(endTime)
											&& lecture.getEndTime().isAfter(startTime)) ||
											(lecture.getStartTime().equals(startTime)
													&& lecture.getEndTime().equals(endTime)))) {

								return true;
							}
						}
					}
				}
			}
		}
		// If no conflicting lectures are found, return true
		return false;
	}

	public LocalTime getStartTime() {
		for (Lecture lecture : Lectures) {
			if (lecture != null) {
				return lecture.getStartTime();
			}
		}
		return null;
	}

	public DayOfWeek getDayOfWeek() {
		for (Lecture lecture : Lectures) {
			if (lecture != null) {
				return lecture.getDayOfWeek();
			}
		}
		return null;
	}
	public void removeLastLecture() {
		if(!this.Lectures.isEmpty()) {
			 this.Lectures.remove(this.Lectures.size()-1);
		}
		
	}

}