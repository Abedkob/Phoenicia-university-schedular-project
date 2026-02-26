import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Doctor {
	private String name;
	public Map<DayOfWeek, List<TimeSlot>> availability;
	public Map<DayOfWeek, List<TimeSlot>> scheduledTimes;
	public Map<DayOfWeek, Set<TimeSlot>> availableTimeSlots;

	public Doctor(String name) {
		this.name = name;
		this.availability = new HashMap<>();
		this.scheduledTimes = new HashMap<>();
		this.availableTimeSlots = new HashMap<>();
	}

	public boolean isAvailable(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		List<TimeSlot> dayAvailability = availability.get(dayOfWeek);
		List<TimeSlot> dayScheduled = scheduledTimes.getOrDefault(dayOfWeek, new ArrayList<>());
		if (dayAvailability == null) {
			return false;
		}
		for (TimeSlot slot : dayAvailability) {
			if (slot.getStartTime().compareTo(startTime) <= 0 && slot.getEndTime().compareTo(endTime) >= 0 &&
					!hasConflict(dayScheduled, startTime, endTime)) {
				return true;
			}
		}
		return false;
	}

	public void addAvailability(DayOfWeek day, LocalTime startTime, LocalTime endTime) {
		if (!availability.containsKey(day)) {
			availability.put(day, new ArrayList<>());
		}

		// Check for overlap with existing availability slots
		List<TimeSlot> dayAvailability = availability.get(day);
				for (TimeSlot existingSlot : dayAvailability) {
					if (existingSlot.overlaps(startTime, endTime)) {
						// Overlap detected, handle accordingly (maybe throw an exception or merge
						// overlapping slots)
						// For simplicity, let's throw an IllegalArgumentException here
						throw new IllegalArgumentException("Overlap detected with existing availability slots");
					}
				}

				// No overlap, add the new availability slot
				availability.get(day).add(new TimeSlot(startTime, endTime));

	}

	public void addScheduledTime(DayOfWeek day, LocalTime startTime, LocalTime endTime) {
		List<TimeSlot> slots = scheduledTimes.computeIfAbsent(day, k -> new ArrayList<>());
		slots.add(new TimeSlot(startTime, endTime));
	}

	public String getName() {
		return name;
	}

	public Map<DayOfWeek, Set<TimeSlot>> getAvailableTimeSlots() {
		return availableTimeSlots;
	}

	public void setAvailableTimeSlots(Map<DayOfWeek, Set<TimeSlot>> availableTimeSlots) {
		this.availableTimeSlots = availableTimeSlots;
	}

	public boolean isScheduled(DayOfWeek day, LocalTime startTime, LocalTime endTime) {
		if (scheduledTimes.containsKey(day)) {
			List<TimeSlot> slots = scheduledTimes.get(day);
			for (TimeSlot slot : slots) {
				if (!slot.getEndTime().isBefore(startTime) && !slot.getStartTime().isAfter(endTime)) {
					return true; // Doctor is scheduled for any part of the specified time range
				}
			}
		}
		return false; // Doctor is not scheduled for the specified time range
	}

	public Map<DayOfWeek, List<TimeSlot>> getAvailability() {
		return availability;
	}

	public boolean hasConflict(List<TimeSlot> scheduledTimes, LocalTime startTime, LocalTime endTime) {
		for (TimeSlot slot : scheduledTimes) {
			if (slot.overlaps(startTime, endTime)) {
				return true;
			}
		}
		return false;
	}

	public void addBack(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		List<TimeSlot> slots = scheduledTimes.get(dayOfWeek); // Get the list of time slots for the specified day
		if (slots != null) { // Check if the list is not null
			// Iterate through the list of time slots to find and remove the matching time
			// slot
			Iterator<TimeSlot> iterator = slots.iterator();
			while (iterator.hasNext()) {
				TimeSlot timeSlot = iterator.next();
				if (timeSlot.getStartTime().equals(startTime) && timeSlot.getEndTime().equals(endTime)) {
					iterator.remove();
					break;
				}
			}
		}
	}

}