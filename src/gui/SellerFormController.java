package gui;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import db.DbException;
import gui.listeners.DataChangeListener;
import gui.util.Alerts;
import gui.util.Constraints;
import gui.util.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import model.entities.Department;
import model.entities.Seller;
import model.exceptions.ValidationException;
import model.services.DepartmentService;
import model.services.SellerService;

public class SellerFormController implements Initializable {

	private Seller entity;
	private SellerService sellerService;
	private DepartmentService departmentService;
	private List<DataChangeListener> dataChangeListeners = new ArrayList<>();

	@FXML
	private TextField txtId;
	@FXML
	private TextField txtName;
	@FXML
	private TextField txtEmail;
	@FXML
	private DatePicker dpBirthDate;
	@FXML
	private TextField txtBaseSalary;

	@FXML
	private ComboBox<Department> comboBoxDepartment;

	@FXML
	private Label labelErrorName;
	@FXML
	private Label labelErrorEmail;
	@FXML
	private Label labelErrorBirthDate;
	@FXML
	private Label labelErrorBaseSalary;

	@FXML
	private Button btSave;
	@FXML
	private Button btCancel;

	@FXML
	private ObservableList<Department> obsList;

	public void setSeller(Seller entity) {
		this.entity = entity;

	}

	public void setServices(SellerService sellerService, DepartmentService departmentService) {
		this.sellerService = sellerService;
		this.departmentService = departmentService;
	}

	public void subscribeDataChangeListener(DataChangeListener listener) {
		dataChangeListeners.add(listener);
	}

	public void updateFormData() {
		if (entity == null) {
			throw new IllegalStateException("Entity was null");
		}
		txtId.setText(String.valueOf(entity.getId()));
		txtName.setText(entity.getName());
		txtEmail.setText(entity.getEmail());
		Locale.setDefault(Locale.US);
		txtBaseSalary.setText(String.format("%.2f", entity.getBaseSalary()));
		if (entity.getBirthDate() != null) {
			dpBirthDate.setValue(LocalDate.ofInstant(entity.getBirthDate().toInstant(), ZoneId.systemDefault()));
		}
		if (entity.getDepartment() == null) {
			comboBoxDepartment.getSelectionModel().selectFirst();
		} else comboBoxDepartment.setValue(entity.getDepartment());		
	}

	public void loadAssociatedObjects() {
		Alerts.injectedCorrectly(departmentService); // Programa��o defensiva: testa que objeto n�o � nulo
		List<Department> depList = departmentService.findAll();
		obsList = FXCollections.observableArrayList(depList);
		comboBoxDepartment.setItems(obsList);
	}

	@FXML
	public void onBtSaveAction(ActionEvent event) {
		if (entity == null) {
			throw new IllegalStateException("Entity was null");
		}
		if (sellerService == null) {
			throw new IllegalStateException("Service was null");
		}

		try {
			sellerService.SaveOrUpdate(getFormData());
			notifyDataChangeListeners();
			Utils.currentStage(event).close();
		} catch (ValidationException e) {
			setErrorMessages(e.getErrors());
		} catch (DbException e) {
			Alerts.showAlert("Error saving Object", null, e.getMessage(), AlertType.ERROR);
		}
	}

	private void notifyDataChangeListeners() {
		for (DataChangeListener listener : dataChangeListeners) {
			listener.onDataChenged();
		}
	}

	private Seller getFormData() {
		Seller seller = new Seller();
		ValidationException exception = new ValidationException("Validation error");
		
		seller.setId(Utils.tryParseToInt(txtId.getText()));
		
		if (txtName.getText() == null || txtName.getText().trim().equals("")) {
			exception.addError("name", "  Field can't be empty");
		} else seller.setName(txtName.getText());
		
		if (txtEmail.getText() == null || txtEmail.getText().trim().equals("")) {
			exception.addError("email", "  Field can't be empty");
		} else seller.setEmail(txtEmail.getText());
		
		// Convertendo "Instant" do form para "Date" do Objeto do "Model"
		if(dpBirthDate.getValue() == null) {
			exception.addError("birthDate", "  Field can't be empty");
		}else {
			Instant instant = Instant.from(dpBirthDate.getValue().atStartOfDay(ZoneId.systemDefault()));
			seller.setBirthDate(Date.from(instant));
		 }
		if (txtBaseSalary.getText() == null || txtBaseSalary.getText().trim().equals("")) {
			exception.addError("baseSalary", "  Field can't be empty");
		} else seller.setBaseSalary(Utils.tryParseToDouble(txtBaseSalary.getText()));
		
		seller.setDepartment(comboBoxDepartment.getValue());		
		
		if (exception.getErrors().size() > 0) {
			throw exception;
		}
		return seller;
	}

	@FXML
	public void onBtCancelAction(ActionEvent event) {
		Utils.currentStage(event).close();
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		initializeNodes();
	}

	private void initializeNodes() {
		Constraints.setTextFieldInteger(txtId);
		Constraints.setTextFieldMaxLength(txtName, 70);
		Constraints.setTextFieldDouble(txtBaseSalary);
		Constraints.setTextFieldMaxLength(txtEmail, 60);
		Utils.formatDatePicker(dpBirthDate, "dd/MM/yyyy");
		
		initializeComboBoxDepartment();
	}

	private void setErrorMessages(Map<String, String> errors) {
		Set<String> fields = errors.keySet();
		
		labelErrorName.setText(fields.contains("name") ? errors.get("name") : "");
		labelErrorEmail.setText(fields.contains("email") ? errors.get("email") : "");
		labelErrorBirthDate.setText(fields.contains("birthDate") ? errors.get("birthDate") : "");
		labelErrorBaseSalary.setText(fields.contains("baseSalary") ? errors.get("baseSalary") : "");
		
//		if (fields.contains("name")) {
//			labelErrorName.setText(errors.get("name"));
//		}else {
//			labelErrorName.setText("");
//		}
//		if (fields.contains("Email")) {
//			labelErrorEmail.setText(errors.get("email"));
//		}else {
//			labelErrorEmail.setText("");
//		}
//		if (fields.contains("birthDate")) {
//			labelErrorBirthDate.setText(errors.get("birthDate"));
//		}else {
//			labelErrorBirthDate.setText("");
//		}
//		if (fields.contains("baseSalary")) {
//			labelErrorBaseSalary.setText(errors.get("baseSalary"));
//		}else {
//			labelErrorBaseSalary.setText("");
//		}
	}

	private void initializeComboBoxDepartment() {
		Callback<ListView<Department>, ListCell<Department>> factory = lv -> new ListCell<Department>() {
			@Override
			protected void updateItem(Department item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? "" : item.getName());
			}
		};
		comboBoxDepartment.setCellFactory(factory);
		comboBoxDepartment.setButtonCell(factory.call(null));
	}
}
