package com.malinskiy.marathon.actor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StateMachineTest {
    @Test
    fun `initial state is the state declared at creation`() {
        val machine = createStateMachine()

        assertThat(machine.state).isEqualTo(State.Solid)
    }

    @Test
    fun `event defined for the current state transitions to the new state and emits the side effect`() {
        val machine = createStateMachine()

        val transition = machine.transition(Event.OnMelted)

        assertThat(machine.state).isEqualTo(State.Liquid)
        val valid = transition as StateMachine.Transition.Valid
        assertThat(valid.fromState).isEqualTo(State.Solid)
        assertThat(valid.event).isEqualTo(Event.OnMelted)
        assertThat(valid.toState).isEqualTo(State.Liquid)
        assertThat(valid.sideEffect).isEqualTo(SideEffect.LogMelted)
    }

    @Test
    fun `event without a matching transition returns an invalid transition and keeps the state`() {
        val machine = createStateMachine()

        val transition = machine.transition(Event.OnCondensed)

        assertThat(transition).isInstanceOf(StateMachine.Transition.Invalid::class.java)
        assertThat(machine.state).isEqualTo(State.Solid)
    }

    @Test
    fun `dontTransition keeps the current state and defaults to a null side effect`() {
        val machine = createStateMachine()
        machine.transition(Event.OnMelted)
        machine.transition(Event.OnVaporized)

        val transition = machine.transition(Event.OnHeated(100))

        val valid = transition as StateMachine.Transition.Valid
        assertThat(valid.toState).isEqualTo(State.Gas)
        assertThat(valid.sideEffect).isNull()
        assertThat(machine.state).isEqualTo(State.Gas)
    }

    @Test
    fun `onTransition listener is notified of valid transitions`() {
        val transitions = mutableListOf<StateMachine.Transition<State, Event, SideEffect>>()
        val machine = createStateMachine { transitions.add(it) }

        machine.transition(Event.OnMelted)

        assertThat(transitions).hasSize(1)
        assertThat(transitions.first()).isInstanceOf(StateMachine.Transition.Valid::class.java)
    }

    @Test
    fun `onTransition listener is notified of invalid transitions`() {
        val transitions = mutableListOf<StateMachine.Transition<State, Event, SideEffect>>()
        val machine = createStateMachine { transitions.add(it) }

        machine.transition(Event.OnCondensed)

        assertThat(transitions).hasSize(1)
        assertThat(transitions.first()).isInstanceOf(StateMachine.Transition.Invalid::class.java)
    }

    @Test
    fun `enter and exit listeners are notified in order on a valid transition`() {
        val notifications = mutableListOf<String>()
        val machine = createMachineWithListeners(notifications)

        machine.transition(Event.OnMelted)

        assertThat(notifications).containsExactly("exit", "enter")
    }

    @Test
    fun `enter and exit listeners are not notified for an invalid transition`() {
        val notifications = mutableListOf<String>()
        val machine = createMachineWithListeners(notifications)

        machine.transition(Event.OnFrozen)

        assertThat(notifications).isEmpty()
    }

    @Test
    fun `with creates an independent copy starting from the current state`() {
        val machine = createStateMachine()
        machine.transition(Event.OnMelted)

        val copy = machine.with { }
        copy.transition(Event.OnVaporized)

        assertThat(copy.state).isEqualTo(State.Gas)
        assertThat(machine.state).isEqualTo(State.Liquid)
    }

    @Test
    fun `event matcher predicate gates the transition`() {
        val machine = StateMachine.create<State, Event, SideEffect> {
            initialState(State.Liquid)
            state<State.Liquid> {
                on(any<Event.OnHeated>().where { temperature >= 100 }) {
                    transitionTo(State.Gas, SideEffect.LogVaporized)
                }
            }
            state<State.Gas> { }
        }

        val notMatched = machine.transition(Event.OnHeated(99))
        val matched = machine.transition(Event.OnHeated(100))

        assertThat(notMatched).isInstanceOf(StateMachine.Transition.Invalid::class.java)
        assertThat(matched).isInstanceOf(StateMachine.Transition.Valid::class.java)
        assertThat(machine.state).isEqualTo(State.Gas)
    }

    @Test
    fun `transition defined for a specific event value matches only that event`() {
        val machine = StateMachine.create<State, Event, SideEffect> {
            initialState(State.Liquid)
            state<State.Liquid> {
                on(Event.OnHeated(100)) {
                    transitionTo(State.Gas, SideEffect.LogVaporized)
                }
            }
            state<State.Gas> { }
        }

        val notMatched = machine.transition(Event.OnHeated(50))
        val matched = machine.transition(Event.OnHeated(100))

        assertThat(notMatched).isInstanceOf(StateMachine.Transition.Invalid::class.java)
        assertThat(matched).isInstanceOf(StateMachine.Transition.Valid::class.java)
    }

    @Test
    fun `first matching transition definition wins`() {
        val machine = StateMachine.create<State, Event, SideEffect> {
            initialState(State.Liquid)
            state<State.Liquid> {
                on(Event.OnHeated(100)) {
                    transitionTo(State.Gas, SideEffect.LogVaporized)
                }
                on<Event.OnHeated> {
                    dontTransition()
                }
            }
            state<State.Gas> { }
        }

        val specific = machine.transition(Event.OnHeated(100))

        assertThat((specific as StateMachine.Transition.Valid).toState).isEqualTo(State.Gas)
    }

    @Test
    fun `transition from a state without a definition throws`() {
        val machine = StateMachine.create<State, Event, SideEffect> {
            initialState(State.Solid)
            state<State.Liquid> { }
        }

        assertThrows<IllegalStateException> { machine.transition(Event.OnMelted) }
    }

    @Test
    fun `machine without an initial state cannot be created`() {
        assertThrows<IllegalArgumentException> {
            StateMachine.create<State, Event, SideEffect> {
                state<State.Solid> { }
            }
        }
    }

    private fun createStateMachine(
        listener: (StateMachine.Transition<State, Event, SideEffect>) -> Unit = {}
    ): StateMachine<State, Event, SideEffect> =
        StateMachine.create {
            initialState(State.Solid)
            state<State.Solid> {
                on<Event.OnMelted> {
                    transitionTo(State.Liquid, SideEffect.LogMelted)
                }
            }
            state<State.Liquid> {
                on<Event.OnFrozen> {
                    transitionTo(State.Solid, SideEffect.LogFrozen)
                }
                on<Event.OnVaporized> {
                    transitionTo(State.Gas, SideEffect.LogVaporized)
                }
            }
            state<State.Gas> {
                on<Event.OnCondensed> {
                    transitionTo(State.Liquid, SideEffect.LogCondensed)
                }
                on<Event.OnHeated> {
                    dontTransition()
                }
            }
            onTransition(listener)
        }

    private fun createMachineWithListeners(notifications: MutableList<String>): StateMachine<State, Event, SideEffect> =
        StateMachine.create {
            initialState(State.Solid)
            state<State.Solid> {
                onExit { notifications.add("exit") }
                on<Event.OnMelted> {
                    transitionTo(State.Liquid, SideEffect.LogMelted)
                }
            }
            state<State.Liquid> {
                onEnter { notifications.add("enter") }
            }
        }

    private sealed class State {
        data object Solid : State()
        data object Liquid : State()
        data object Gas : State()
    }

    private sealed class Event {
        data object OnMelted : Event()
        data object OnFrozen : Event()
        data object OnVaporized : Event()
        data object OnCondensed : Event()
        data class OnHeated(val temperature: Int) : Event()
    }

    private sealed class SideEffect {
        data object LogMelted : SideEffect()
        data object LogFrozen : SideEffect()
        data object LogVaporized : SideEffect()
        data object LogCondensed : SideEffect()
    }
}
