import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CSched extends JFrame {

	private CourseScheduler scheduler;
	private DefaultTableModel tableModel;

	public CSched(CourseScheduler scheduler) {
		this.scheduler = scheduler;

		// Create a banner panel with a JLabel and ImageIcon
		JPanel bannerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bannerPanel.setBackground(new Color(0x722F37));

		// Load the image
		ImageIcon bannerImageIcon = new ImageIcon("PU-logo.png");

		// Scale the ImageIcon to the desired width and height
		Image scaledImage = bannerImageIcon.getImage().getScaledInstance(130, 120, Image.SCALE_SMOOTH);
		bannerImageIcon = new ImageIcon(scaledImage);

		// Create a JLabel with the image
		JLabel imageLabel = new JLabel(bannerImageIcon);

		// Create a JLabel with the text
		JLabel textLabel = new JLabel("Course Scheduler");
		textLabel.setForeground(Color.WHITE);
		textLabel.setFont(new Font("Roboto", Font.BOLD, 24));

		// Add both JLabels to the banner panel
		bannerPanel.add(imageLabel);
		bannerPanel.add(textLabel);

		// Create a panel for buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBackground(Color.WHITE);

		// Create a filter button
		JButton filterButton = new JButton("Filter");
		filterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showFilterDialog();
			}
		});

		// Add the filter button to the button panel
		buttonPanel.add(filterButton);

		// Create the main panel for the table
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(new Color(255, 255, 255));

		// Create table model with headers for days of the week
		String[] headers = new String[6]; // Excluding Saturday and Sunday
		headers[0] = "Time"; // Time column
		int index = 1;
		for (DayOfWeek day : DayOfWeek.values()) {
			if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
				headers[index++] = day.toString();
			}
		}

		tableModel = new DefaultTableModel(new Object[0][headers.length], headers);
		JTable table = new JTable(tableModel);
		table.setFont(new Font("Roboto", Font.PLAIN, 16));
		table.setRowHeight(300);
		table.getTableHeader().setFont(new Font("Roboto", Font.BOLD, 16));
		table.getTableHeader().setBackground(new Color(0, 150, 136));
		table.getTableHeader().setForeground(Color.BLACK);
		table.setBackground(new Color(255, 255, 255));
		table.setForeground(Color.BLACK);
		table.setGridColor(Color.LIGHT_GRAY);

		// Populate the table with time slots and courses
		LocalTime[] timeSlots = { LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(11, 0),
				LocalTime.of(13, 0), LocalTime.of(14, 30), LocalTime.of(16, 0) };

		for (LocalTime time : timeSlots) {
			Object[] rowData = new Object[6]; // Excluding Saturday and Sunday
			rowData[0] = time.toString();
			for (int i = 1; i < headers.length; i++) {
				DayOfWeek day = DayOfWeek.values()[i];
				List<Course> coursesAtSameTimeForDay = scheduler.getSchedule().getOrDefault(day, Map.of())
						.getOrDefault(time, new ArrayList<>());
				StringBuilder courseInfo = new StringBuilder("<html><div style='text-align: left; font-size: 11px;'>");
				for (Course course : coursesAtSameTimeForDay) {
					courseInfo.append("&#8226; ").append(course.getDoctorName()).append(": ").append(course.getName())
					.append("<br>");
				}
				courseInfo.append("</div></html>");
				rowData[i] = courseInfo.toString();
			}
			tableModel.addRow(rowData);
		}

		// Set table cell renderer to display multiline text
		DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
		cellRenderer.setVerticalAlignment(SwingConstants.CENTER);
		cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		table.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
		for (int i = 1; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
		}

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// Add the banner panel, button panel, and main panel to the frame
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(bannerPanel, BorderLayout.NORTH);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(mainPanel, BorderLayout.CENTER);

		setTitle("Course Scheduler");
		setSize(1200, 800);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private void showFilterDialog() {
		JDialog dialog = new JDialog(this, "Filter Courses", true);
		dialog.setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(0, 1));
		JScrollPane scrollPane = new JScrollPane(panel);
		dialog.add(scrollPane, BorderLayout.CENTER);

		// Add checkboxes for each course
		for (Course course : scheduler.getCourses()) {
			JCheckBox checkBox = new JCheckBox(course.getName());
			panel.add(checkBox);
		}

		JButton applyButton = new JButton("Apply");
		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<Course> selectedCourses = new ArrayList<>();
				Component[] components = panel.getComponents();
				for (Component component : components) {
					if (component instanceof JCheckBox) {
						JCheckBox checkBox = (JCheckBox) component;
						if (checkBox.isSelected()) {
							String courseName = checkBox.getText();
							// Find the course object by name
							for (Course course : scheduler.getCourses()) {
								if (course.getName().equals(courseName)) {
									selectedCourses.add(course);
									break;
								}
							}
						}
					}
				}
				updateTable(selectedCourses);
				dialog.dispose();
			}
		});

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(applyButton);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		// Set preferred size to widen the dialog
		dialog.setPreferredSize(new Dimension(400, 300));

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void updateTable(List<Course> coursesToShow) {
		// Clear the table
		tableModel.setRowCount(0);

		// Populate the table with time slots and courses
		LocalTime[] timeSlots = { LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(11, 0),
				LocalTime.of(13, 0), LocalTime.of(14, 30), LocalTime.of(16, 0) };

		for (LocalTime time : timeSlots) {
			Object[] rowData = new Object[tableModel.getColumnCount()]; // Adjusted to fit all columns
			rowData[0] = time.toString();
			for (DayOfWeek day : DayOfWeek.values()) {
				if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
					int columnIndex = getColumnIndex(day);
					List<Course> coursesAtSameTimeForDay = scheduler.getSchedule().getOrDefault(day, Map.of())
							.getOrDefault(time, new ArrayList<>());
					StringBuilder courseInfo = new StringBuilder(
							"<html><div style='text-align: left; font-size: 11px;'>");
					for (Course course : coursesAtSameTimeForDay) {
						if (coursesToShow.isEmpty() || coursesToShow.contains(course)) {
							courseInfo.append("&#8226; ").append(course.getDoctorName()).append(": ")
							.append(course.getName())
							.append("<br>");
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
		// Get the index of the day of the week in the table headers
		String[] headers = { "Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].equalsIgnoreCase(dayOfWeek.toString())) {
				return i;
			}
		}
		return -1; // Not found
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		CourseScheduler scheduler = new CourseScheduler(null,8);
		SwingUtilities.invokeLater(() -> new CSched(scheduler));
	}
}
