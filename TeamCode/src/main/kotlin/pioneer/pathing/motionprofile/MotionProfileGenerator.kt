package pioneer.pathing.motionprofile

import pioneer.helpers.MathUtils
import pioneer.pathing.motionprofile.constraints.AccelerationConstraint
import pioneer.pathing.motionprofile.constraints.VelocityConstraint
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

private data class EvaluatedConstraint(
    val maxVel: Double,
    val maxAccel: Double,
)

/**
 * Singleton object for generating motion profiles.
 */
object MotionProfileGenerator {
    /**
     * Generates a motion profile from the given start and end states,
     * applying the specified velocity and acceleration constraints.
     * Accelerates as much as possible within the constraints,
     * Uses a forward and backward pass to ensure the constraints.
     */
    fun generateMotionProfile(
        startState: MotionState,
        endState: MotionState,
        velocityConstraint: VelocityConstraint,
        accelerationConstraint: AccelerationConstraint,
        resolution: Double = 0.1, // Length of each segment in cm
    ): MotionProfile {
        if (abs(startState.x - endState.x) < 1e-3) {
            return MotionProfile(
                listOf(
                    MotionSegment(startState, 0.0),
                ),
            )
        }

        // Get the arc length between start and end states
        val length = endState.x - startState.x
        // Calculate the number of samples based on the resolution
        // At least two samples are required to have a valid profile
        val samples = max(2, ceil(length / resolution).toInt())

        // Generate a list of evenly spaced points along the path
        val displacements = MathUtils.linspace(startState.x, endState.x, samples)
        // Evaluate the constraints at each point
        val constraintsList =
            displacements.map { s ->
                val maxVel = velocityConstraint[s]
                val maxAccel = accelerationConstraint[s]
                EvaluatedConstraint(maxVel, maxAccel)
            }

        // Forward pass to calculate motion states
        val forwardStates =
            forwardPass(
                startState,
                displacements,
                constraintsList,
            ).toMutableList()

        // Backward pass to calculate motion states
        val backwardStates =
            backwardPass(
                endState,
                displacements,
                constraintsList,
            ).toMutableList()

        // Combine the forward and backward states
        val finalStates = mutableListOf<Pair<MotionState, Double>>()
        var i = 0
        while (i < forwardStates.size && i < backwardStates.size) {
            var (forwardStartState, forwardDx) = forwardStates[i]
            var (backwardEndState, backwardDx) = backwardStates[i]

            // Ensure the forward and backward states match
            if (!(abs(forwardDx - backwardDx) < 1e-6)) {
                if (forwardDx > backwardDx) {
                    // Forward segment is longer
                    // Adjust the forward state to match the backward state
                    forwardStates.add(
                        i + 1,
                        Pair(afterDisplacement(forwardStartState, backwardDx), forwardDx - backwardDx),
                    )
                    forwardDx = backwardDx
                } else {
                    // Backward segment is longer
                    // Adjust the backward state to match the forward state
                    backwardStates.add(
                        i + 1,
                        Pair(afterDisplacement(backwardEndState, forwardDx), backwardDx - forwardDx),
                    )
                    backwardDx = forwardDx
                }
            }
            // Calculate the end states for this segment
            val forwardEndState = afterDisplacement(forwardStartState, forwardDx)
            val backwardStartState = afterDisplacement(backwardEndState, backwardDx)

            // Choose the state with the lower velocity as the final state
            // If the lower velocity changes from the start to the end of the segment, split the segment based on the intersection
            if (forwardStartState.v <= backwardStartState.v) {
                if (forwardEndState.v <= backwardStartState.v) {
                    // Both the start and end state of the forward segment are lower
                    finalStates.add(Pair(forwardStartState, forwardDx))
                } else {
                    // The forward start state is lower, but the end state is higher
                    val intersection = intersection(forwardStartState, backwardStartState)
                    finalStates.add(Pair(forwardStartState, intersection))
                    finalStates.add(
                        Pair(
                            afterDisplacement(backwardStartState, intersection),
                            backwardDx - intersection,
                        ),
                    )
                }
            } else {
                if (backwardEndState.v <= forwardStartState.v) {
                    // Both the start and end state of the backward segment are lower
                    finalStates.add(Pair(backwardEndState, backwardDx))
                } else {
                    // The backward start state is lower, but the end state is higher
                    val intersection = intersection(forwardStartState, backwardStartState)
                    finalStates.add(Pair(backwardStartState, intersection))
                    finalStates.add(
                        Pair(
                            afterDisplacement(forwardStartState, intersection),
                            forwardDx - intersection,
                        ),
                    )
                }
            }
            i++
        }

        // Convert the forward states into a motion profile
        val motionSegments = mutableListOf<MotionSegment>()
        for ((state, dx) in finalStates) {
            // Calculate the time taken for this segment
            val dt =
                if (abs(state.a) < 1e-6) {
                    // If acceleration is zero, use constant velocity to calculate time
                    if (abs(state.v) < 1e-6) 0.0 else dx / state.v
                } else {
                    // Use the kinematic equation to calculate time
                    val discriminant = state.v * state.v + 2 * state.a * dx
                    if (abs(discriminant) < 1e-6) {
                        -state.v / state.a
                    } else {
                        (sqrt(discriminant) - state.v) / state.a
                    }
                }
            // Create a motion segment for this state
            motionSegments.add(MotionSegment(state, dt))
        }
        // Create the motion profile from the segments
        val motionProfile = MotionProfile(motionSegments)
        // Return the generated motion profile
        return motionProfile
    }

    /**
     * Performs a forward pass to calculate the motion state for each displacement.
     * This method applies the constraints to ensure that the motion profile adheres
     * to the maximum velocity and acceleration limits.
     */
    private fun forwardPass(
        startState: MotionState,
        displacements: List<Double>,
        constraints: List<EvaluatedConstraint>,
    ): List<Pair<MotionState, Double>> {
        val forwardStates = mutableListOf<Pair<MotionState, Double>>()
        val dx = displacements[1] - displacements[0] // Assuming uniform spacing
        var currentState = startState

        for (i in displacements.indices) {
            val displacement = displacements[i]
            val constraint = constraints[i]

            // Calculate the maximum velocity and acceleration for this segment
            val maxVel = constraint.maxVel
            val maxAccel = constraint.maxAccel

            if (currentState.v >= maxVel) {
                // If the current velocity exceeds the maximum, cruise at max velocity
                val state = MotionState(displacement, maxVel, 0.0)
                forwardStates.add(Pair(state, dx))
                // Calculate the next state based on the limited velocity
                currentState = afterDisplacement(state, dx)
            } else {
                // If the current velocity is below the maximum, accelerate
                // Calculate the new velocity based on the maximum acceleration
                val newVel = sqrt(currentState.v * currentState.v + 2 * maxAccel * dx)

                if (newVel <= maxVel) {
                    // If the new velocity after acceleration is still within the limit
                    val state = MotionState(displacement, currentState.v, maxAccel)
                    forwardStates.add(Pair(state, dx))
                    currentState = afterDisplacement(state, dx)
                } else {
                    // The velocity exceeds the maximum before reaching the next point
                    // Split the segment into acceleration and cruising phases
                    val accelDx = (maxVel * maxVel - currentState.v * currentState.v) / (2 * maxAccel)
                    val accelState = MotionState(displacement, currentState.v, maxAccel)
                    val cruiseState = MotionState(displacement + accelDx, maxVel, 0.0)
                    forwardStates.add(Pair(accelState, accelDx))
                    forwardStates.add(Pair(cruiseState, dx - accelDx))
                    // Update the current state to the end of the cruise segment
                    currentState = afterDisplacement(cruiseState, dx - accelDx)
                }
            }
        }
        return forwardStates
    }

    /**
     * Performs a backward pass to ensure the motion profile adheres to constraints.
     * This method adjusts the motion states based on the maximum velocity and acceleration
     * limits, ensuring that the profile is valid from end to start.
     */
    private fun backwardPass(
        endState: MotionState,
        displacements: List<Double>,
        constraints: List<EvaluatedConstraint>,
    ): List<Pair<MotionState, Double>> {
        // Reverse the constraints for backward processing
        val reversedPass =
            forwardPass(
                MotionState(0.0, endState.v, endState.a),
                displacements,
                constraints.reversed(),
            )

        // Reverse the states to match the original order
        return reversedPass
            .map { (state, dx) ->
                Pair(afterDisplacement(state, dx), dx)
            }.map { (state, dx) ->
                Pair(MotionState(endState.x - state.x, state.v, -state.a), dx)
            }.reversed()
    }

    /**
     * Calculates the motion state after a displacement.
     * This method uses the current motion state and the displacement to compute
     * the new position and velocity, taking into account the acceleration.
     */
    private fun afterDisplacement(
        state: MotionState,
        dx: Double,
    ): MotionState {
        val discriminant = state.v * state.v + 2 * state.a * dx
        return if (abs(discriminant) < 1e-6) {
            MotionState(state.x + dx, 0.0, state.a)
        } else {
            MotionState(state.x + dx, sqrt(discriminant), state.a)
        }
    }

    /**
     * Calculates the intersection point between two motion states.
     * This method finds the displacement at which the two states would have the same velocity.
     */
    private fun intersection(
        state1: MotionState,
        state2: MotionState,
    ): Double = (state1.v * state1.v - state2.v * state2.v) / (2 * state2.a - 2 * state1.a)
}
