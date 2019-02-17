package frc.robot;

import java.util.function.Supplier;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import frc.robot.autotasks.*;

public class TeleopController {

	CargoMechanismInterface cargo;
	HatchPanelMechanismInterface panel;
	DrivetrainInterface driveTrain;

	public Compressor compressor = new Compressor();

	//Controllers
	private Extreme3DProJoystick stick = new Extreme3DProJoystick(0);
	private LogitechGamepadController gamepad = new LogitechGamepadController(1);

	SendableChooser<String> driveChooser;

	private String driveSelected;
	private boolean isHumanControlled;
	private TaskInterface visionHatchPlaceRoutine;
	private AutoFactory autoFactory;

	private boolean isCargoForward;

	private double rotateMultiplier = 1;
	private double speedMultiplier = 1;

	private final Supplier<Double> rotateMultiplierSupplier;
	private final Supplier<Double> speedMultiplierSupplier;

	public TeleopController(DrivetrainInterface d,
							HatchPanelMechanismInterface p,
							CargoMechanismInterface c,
							AutoFactory a,
							SendableChooser<String> driveChooser,
							SendableChooser<String> operateChooser,
							SendableChooser<String> sideChooser,
							Supplier<Double> rotateMultiplierSupplier,
							Supplier<Double> speedMultiplierSupplier) {
		cargo = c;
		panel = p;
		driveTrain = d;
		autoFactory = a;
		this.driveChooser = driveChooser;

		this.rotateMultiplierSupplier = rotateMultiplierSupplier;
		this.speedMultiplierSupplier = speedMultiplierSupplier;

		compressor.start();

		// Operate
		stick.bindButtonToggle(Extreme3DProJoystick.TOP_RIGHT_TOP_BUTTON,     panel::floorPickup,       panel::stow);
		stick.bindButtonToggle(Extreme3DProJoystick.TOP_LEFT_TOP_BUTTON,      panel::deposit,           panel::stow);
		stick.bindButtonToggle(Extreme3DProJoystick.BOTTOM_LEFT_TOP_BUTTON,   panel::stationPickup,     panel::stow);
		stick.bindButtonPress(Extreme3DProJoystick.BOTTOM_RIGHT_TOP_BUTTON,   panel::stow);

		stick.bindButtonToggle(Extreme3DProJoystick.TRIGGER,                  cargo::shootHigh,         cargo::idle);
		stick.bindButtonToggle(Extreme3DProJoystick.THUMB_BUTTON,             cargo::shootLow,          cargo::idle);
		stick.bindButtonToggle(Extreme3DProJoystick.TOP_LEFT_BASE_BUTTON,     cargo::floorIntakeBarIn,  cargo::idle);
		stick.bindButtonToggle(Extreme3DProJoystick.TOP_RIGHT_BASE_BUTTON,    cargo::floorIntakeBarOut, cargo::idle);
		stick.bindButtonToggle(Extreme3DProJoystick.MIDDLE_LEFT_BASE_BUTTON,  cargo::midIntake,         cargo::idle);
		stick.bindButtonToggle(Extreme3DProJoystick.MIDDLE_RIGHT_BASE_BUTTON, cargo::hoodIntake,        cargo::idle);
		stick.bindButtonToggle(Extreme3DProJoystick.BOTTOM_LEFT_BASE_BUTTON,  cargo::flush,             cargo::idle);
		stick.bindButtonPress(Extreme3DProJoystick.BOTTOM_RIGHT_BASE_BUTTON,  cargo::idle);

		// Drive
		gamepad.bindAxes(gamepad.LEFT_Y_AXIS, gamepad.RIGHT_X_AXIS, this::arcade);
		gamepad.bindButtonPress(gamepad.LEFT_STICK_IN, () -> isCargoForward = !isCargoForward);
		gamepad.bindButtonPress(gamepad.A_BUTTON, () -> isCargoForward = !isCargoForward);
		gamepad.bindButton(gamepad.LEFT_BUMPER, this::quickTurnleft);
		gamepad.bindButton(gamepad.RIGHT_BUMPER, this::quickTurnRight);
		gamepad.bindButtonPress(gamepad.X_BUTTON, this::startVisionHatchTask);
		gamepad.bindButtonPress(gamepad.B_BUTTON, () -> isHumanControlled = true);
	}

	public void teleopInit() {
		isHumanControlled = true;
		isCargoForward = false;
	}

	public void runTeleop() {
		driveSelected = driveChooser.getSelected();

		rotateMultiplier = rotateMultiplierSupplier.get();
		speedMultiplier = speedMultiplierSupplier.get();

		if (!isHumanControlled) {
			if (!visionHatchPlaceRoutine.isFinished()){
				visionHatchPlaceRoutine.execute();
			} else {
				visionHatchPlaceRoutine.end();
				isHumanControlled = true;
			}
		}
		
		gamepad.run();
		stick.run();

		panel.periodic();
		cargo.periodic();
	}

	private void startVisionHatchTask() {
		isHumanControlled = false;
		visionHatchPlaceRoutine = new AutoRoutine(autoFactory.visionHatchPlace());
		visionHatchPlaceRoutine.start();
	}

	private void arcade(double speed, double rotation) {
		double outSpeed = speedMultiplier * speed;
		double outRotation = rotation * rotateMultiplier;
		if (isHumanControlled) {
			if (isCargoForward){
				driveTrain.setArcadeDriveSpeed(outSpeed, -outRotation);
			} else {
				driveTrain.setArcadeDriveSpeed(-outSpeed, -outRotation);
			}
		}
	}

	private void quickTurnleft() {
		driveTrain.setArcadeDriveSpeed(0, 0.5);
	}

	private void quickTurnRight() {
		driveTrain.setArcadeDriveSpeed(0, -0.5);
	}

	private void tank(double left, double right) {
		double outLeft = -left * speedMultiplier;
		double outRight = -right * speedMultiplier;

		driveTrain.setLeftRightDriveSpeed(outLeft, outRight);
	}

	public void startCompressor(){
		compressor.start();
	}
}
