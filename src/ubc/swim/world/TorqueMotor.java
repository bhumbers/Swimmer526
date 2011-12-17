package ubc.swim.world;

public abstract class TorqueMotor {
	/**
	 * Applies torque to the bodies under this motor's control
	 * with the magnitude based on given runtime value.
	 * Returns the magnitude of the applied torque.
	 * @param time
	 */
	public abstract float applyTorque(float runtime);
}
