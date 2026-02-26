import java.time.LocalTime;

public class TimeSlot {
	private LocalTime startTime;
	private LocalTime endTime;
	private boolean lectureScheduled; // New field to track lecture scheduling

	public TimeSlot(LocalTime startTime, LocalTime endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.lectureScheduled = false; // Initialize lectureScheduled to false by default
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public boolean containsTime(LocalTime time) {
		return !time.isBefore(startTime) && !time.isAfter(endTime);
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public boolean isLectureScheduled() {
		return lectureScheduled;
	}

	public boolean overlaps(LocalTime otherStartTime, LocalTime otherEndTime) {
		return this.startTime.isBefore(otherEndTime) && otherStartTime.isBefore(this.endTime);
	}

	public void setLectureScheduled(boolean lectureScheduled) {
		this.lectureScheduled = lectureScheduled;
	}

	public LocalTime getTime() {
		return startTime; // Return the start time as it represents the time of the slot
	}

	public String toString() {
		return startTime.toString() + " - " + endTime.toString();
	}
}