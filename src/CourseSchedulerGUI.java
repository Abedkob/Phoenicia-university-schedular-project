import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CourseSchedulerGUI extends JFrame {
    private CourseScheduler courseScheduler;
    private DefaultTableModel tableModel;

    public CourseSchedulerGUI(CourseScheduler scheduler) {
        this.courseScheduler = scheduler;

        setTitle("Course Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Banner panel
        JPanel bannerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bannerPanel.setBackground(new Color(0x722F37));
        JLabel bannerLabel = new JLabel("Course Scheduler");
        bannerLabel.setForeground(Color.WHITE);
        bannerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        bannerPanel.add(bannerLabel);
        add(bannerPanel, BorderLayout.NORTH);

        // Table setup
        String[] columnNames = {"Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        tableModel = new DefaultTableModel(columnNames, 0);

        JTable table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };
        table.setRowHeight(150);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        table.setSelectionBackground(new Color(0xFFEFD5));

        // Set alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row % 2 == 0) {
                    component.setBackground(new Color(0xF0F0F0));
                } else {
                    component.setBackground(Color.WHITE);
                }
                return component;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton showConflictsButton = new JButton("Show Conflicts");
        JButton filterCoursesButton = new JButton("Filter Courses");
        JButton showUnscheduledButton = new JButton("Show Unscheduled Courses");
		JButton reset = new JButton("Reset");

        showConflictsButton.addActionListener(e -> showConflicts());
        filterCoursesButton.addActionListener(e -> filterCourses());
        showUnscheduledButton.addActionListener(e -> showUnscheduledCourses());
		reset.addActionListener(e -> resetTable());

        buttonPanel.add(showConflictsButton);
        buttonPanel.add(filterCoursesButton);
        buttonPanel.add(showUnscheduledButton);
		buttonPanel.add(reset);
        add(buttonPanel, BorderLayout.SOUTH);

        // Populate table with course data
        populateTable();

        setVisible(true);
    }

	private void resetTable(){
		populateTable();
	}
	private void populateTable() {
		// Clear the table
		tableModel.setRowCount(0);

		// Define the time slots and corresponding end times
		List<TimeSlot> timeSlots = new ArrayList<>();
		timeSlots.add(new TimeSlot(LocalTime.of(8, 0), LocalTime.of(8, 50)));
		timeSlots.add(new TimeSlot(LocalTime.of(8, 0), LocalTime.of(9, 15)));
		timeSlots.add(new TimeSlot(LocalTime.of(8, 0), LocalTime.of(10, 0)));
		timeSlots.add(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(9, 50)));
		timeSlots.add(new TimeSlot(LocalTime.of(9, 30), LocalTime.of(10, 45)));
		timeSlots.add(new TimeSlot(LocalTime.of(10, 0), LocalTime.of(10, 50)));
		timeSlots.add(new TimeSlot(LocalTime.of(10, 15), LocalTime.of(12, 15)));
		timeSlots.add(new TimeSlot(LocalTime.of(10, 30), LocalTime.of(11, 20)));
		timeSlots.add(new TimeSlot(LocalTime.of(11, 0), LocalTime.of(11, 50)));
		timeSlots.add(new TimeSlot(LocalTime.of(11, 0), LocalTime.of(12, 15)));
		timeSlots.add(new TimeSlot(LocalTime.of(13, 0), LocalTime.of(13, 50)));
		timeSlots.add(new TimeSlot(LocalTime.of(13, 0), LocalTime.of(14, 15)));
		timeSlots.add(new TimeSlot(LocalTime.of(13, 30), LocalTime.of(15, 0)));
		timeSlots.add(new TimeSlot(LocalTime.of(14, 0), LocalTime.of(14, 50)));
		timeSlots.add(new TimeSlot(LocalTime.of(14, 30), LocalTime.of(15, 20)));
		timeSlots.add(new TimeSlot(LocalTime.of(14, 30), LocalTime.of(15, 45)));
		timeSlots.add(new TimeSlot(LocalTime.of(15, 0), LocalTime.of(15, 50)));
		timeSlots.add(new TimeSlot(LocalTime.of(15, 15), LocalTime.of(17, 30)));
		timeSlots.add(new TimeSlot(LocalTime.of(15, 30), LocalTime.of(16, 20)));
		timeSlots.add(new TimeSlot(LocalTime.of(16, 0), LocalTime.of(16, 50)));
		timeSlots.add(new TimeSlot(LocalTime.of(16, 0), LocalTime.of(17, 15)));

		// Populate the table with time slots and scheduled courses
		for (TimeSlot timeSlot : timeSlots) {
			LocalTime startTime = timeSlot.getStartTime();
			LocalTime endTime = timeSlot.getEndTime();

			Object[] rowData = new Object[tableModel.getColumnCount()];
			rowData[0] = startTime.toString() + " - " + endTime.toString();

			for (DayOfWeek day : DayOfWeek.values()) {
				if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
					int columnIndex = getColumnIndex(day);
					List<Course> coursesAtSameTimeForDay = courseScheduler.getSchedule().getOrDefault(day, Map.of())
							.getOrDefault(startTime, new ArrayList<>());

					StringBuilder courseInfo = new StringBuilder(
							"<html><div style='text-align: left; font-size: 11px;'>");

					for (Course course : coursesAtSameTimeForDay) {
						// Check if the course's end time matches the current time slot's end time
						LocalTime courseEndTime = startTime.plusMinutes((long) course.getDurationMinutes());
						if (courseEndTime.equals(endTime)) {
							// Add only scheduled courses to the table
							if (course.getLecturesPerWeek() == 0) {
								courseInfo.append("&#8226; ").append(course.getDoctorName()).append(": ")
								.append(course.getName())
								.append("<br>");
							}
						}
					}
					courseInfo.append("</div></html>");
					rowData[columnIndex] = courseInfo.toString();
				}
			}
			tableModel.addRow(rowData);
		}
	}

	
	private int getColumnIndex(DayOfWeek dayOfWeek) {
		switch (dayOfWeek) {
			case MONDAY: return 1;
			case TUESDAY: return 2;
			case WEDNESDAY: return 3;
			case THURSDAY: return 4;
			case FRIDAY: return 5;
			default: return -1; // Saturday and Sunday, or error
		}
	}
	

    private void showConflicts() {
        JDialog conflictDialog = new JDialog(this, "Conflicting Courses", true);
        JPanel conflictPanel = new JPanel(new BorderLayout());
        JTextArea conflictTextArea = new JTextArea();
        conflictTextArea.setEditable(false);
        conflictTextArea.setFont(new Font("Roboto", Font.PLAIN, 14));

        List<Course> allCourses = new ArrayList<>();
        for (Map<LocalTime, List<Course>> schedule : courseScheduler.getSchedule().values()) {
            for (List<Course> courses : schedule.values()) {
                allCourses.addAll(courses);
            }
        }

        for (Course course : allCourses) {
            Set<String> conflictNames = course.getConflicts();
            conflictTextArea.append("Conflicts for " + course.getName() + ":\n");
            for (String conflictingCourseName : conflictNames) {
                conflictTextArea.append(" - " + conflictingCourseName + "\n");
            }
            conflictTextArea.append("\n");
        }

        JScrollPane scrollPane = new JScrollPane(conflictTextArea);
        conflictPanel.add(scrollPane, BorderLayout.CENTER);
        conflictDialog.add(conflictPanel);
        conflictDialog.setSize(400, 300);
        conflictDialog.setLocationRelativeTo(this);
        conflictDialog.setVisible(true);
    }

    private void filterCourses() {
        String filter = JOptionPane.showInputDialog(this, "Enter course name to filter:");
        if (filter != null && !filter.trim().isEmpty()) {
            List<Course> filteredCourses = courseScheduler.getCourses().stream()
                    .filter(course -> course.getName().toLowerCase().contains(filter.toLowerCase()))
                    .collect(Collectors.toList());
            tableModel.setRowCount(0);
            for (Course course : filteredCourses) {
                tableModel.addRow(new Object[]{
                        course.getName(),
                        course.getInitialLecturesPerWeek(),
                        course.getLecturesPerWeek(),
                        course.getLectures().size()
                });
            }
        }
    }

    private void showUnscheduledCourses() {
        List<Course> unscheduledCourses = courseScheduler.getUnscheduledCourses();
        tableModel.setRowCount(0);
        for (Course course : unscheduledCourses) {
            tableModel.addRow(new Object[]{
                    course.getName(),
                    course.getInitialLecturesPerWeek(),
                    course.getLecturesPerWeek(),
                    course.getLectures().size()
            });
        }
    }

    public static void main(String[] args) {
        CourseScheduler scheduler = attemptScheduling("Data.xlsx");

        if (scheduler != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    new CourseSchedulerGUI(scheduler);
                } catch (Exception e) {
                    System.err.println("Exception in creating GUI: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            System.err.println("Failed to schedule courses. Scheduler object is null.");
        }
    }

	private static CourseScheduler attemptScheduling(String filePath) {
        final int MAX_UNSCHEDULED = 5;
        int count = 0;
        CourseScheduler scheduler = null;
        boolean flag = false;
        int min =1200;
        long seed = 0;
         int minP = 1100;

        try {
            while (true) {
            	seed = System.currentTimeMillis();
                count++;
                if (!flag) {
                    scheduler = new CourseScheduler(filePath,seed);
                    scheduler.readDataFromExcel(filePath);
                } else {
                    break;
                }
                List<Course> unscheduledCourses = scheduler.getUnscheduledCourses();
                List<Course> courses = scheduler.getCourses();
          
                if(unscheduledCourses.size() <min){
                	min = unscheduledCourses.size();
                	System.out.println(min);
                }
                if (unscheduledCourses.size() <= MAX_UNSCHEDULED) {
                    System.out.println("The Program is now Finished After Trying different " + count + " Schedules");
                    for (Course c : courses) {
                        c.displayLectures();
                    }

                    for (Course c : unscheduledCourses) {
                        System.out.println(c.getName() + " Initial Lectures: " + c.getInitialLecturesPerWeek()
                                + " And Lectures Per Week: " + c.getLecturesPerWeek()
                                + " " + c.getLectures().size());
                    }

                    System.out.println(unscheduledCourses.size());
                    flag = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return scheduler;
    }
}