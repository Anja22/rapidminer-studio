/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.usagestats;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.AbstractLinkButton;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.container.Pair;


/**
 * Collects aggregated usage records of different keys. Keys are formed from TYPE, VALUE, ARG to
 * define the 3 possible levels of aggregation. A Key also defines an aggregator function that
 * controls how a new value is combined to the existing aggregate.
 *
 * @author Simon Fischer, Peter Toth
 */
public enum ActionStatisticsCollector {

	INSTANCE;

	public static final String TYPE_CONSTANT = "rapidminer";
	private static final String TYPE_DOCKABLE = "dockable";
	private static final String TYPE_ACTION = "action";
	public static final String TYPE_OPERATOR = "operator";
	public static final String TYPE_PERSPECTIVE = "perspective";
	public static final String TYPE_ERROR = "error";
	public static final String TYPE_IMPORT = "import";
	public static final String TYPE_DIALOG = "dialog";
	public static final String TYPE_CONSTRAINT = "constraint";
	public static final String TYPE_LICENSE_LEVEL = "license-level";
	public static final String TYPE_PROGRESS_THREAD = "progress-thread";
	public static final String TYPE_TEMPLATE = "template";
	public static final String TYPE_RENDERER = "renderer";
	public static final String TYPE_CHART = "chart";

	/** new data access dialog (since 7.0.0) */
	public static final String TYPE_NEW_IMPORT = "new_import";

	/** start-up dialog (since 7.0.0) */
	public static final String TYPE_GETTING_STARTED = "getting_started";

	/** operator search field (since 7.1.1) */
	public static final String TYPE_OPERATOR_SEARCH = "operator_search";

	/** onboarding dialog (since 7.1.1) */
	public static final String TYPE_ONBOARDING = "onboarding";

	public static final String TYPE_NEWSFETCH = "news-fetch";

	public static final String VALUE_NEWSFETCH_START = "start";
	public static final String VALUE_NEWSFETCH_ERROR = "error";
	public static final String VALUE_NEWSFETCH_SUCCESS = "success";
	public static final String VALUE_NEWSFETCH_HTTPRESULT = "http-result";
	public static final String VALUE_NEWSFETCH_HTTPERROR = "http-error";

	public static final String OPERATOR_EVENT_EXECUTION = "EXECUTE";
	public static final String OPERATOR_EVENT_STOPPED = "STOPPED";
	public static final String OPERATOR_EVENT_FAILURE = "FAILURE";
	public static final String OPERATOR_EVENT_USER_ERROR = "USER_ERROR";
	public static final String OPERATOR_EVENT_OPERATOR_EXCEPTION = "OPERATOR_EXCEPTION";
	public static final String OPERATOR_EVENT_RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION";

	/** runtime of an operator (since 7.1.1) */
	private static final String OPERATOR_RUNTIME = "RUNTIME";

	/** input and output volume of an operator port (since 7.1.1) */
	private static final String TYPE_INPUT_VOLUME = "input_volume";
	private static final String TYPE_OUTPUT_VOLUME = "output_volume";

	/** jvm total memory logging (since 7.1.1) */
	private static final String TYPE_MEMORY = "memory";
	private static final String MEMORY_USED = "used";
	private static final String MEMORY_ARG = "MEMORY";

	/** arguments to log operator port volume, cells = columns*rows, (since 7.1.1) */
	private static final String VOLUMNE_CELLS = "CELLS";
	private static final String VOLUME_COLUMNS = "COLUMNS";
	private static final String VOLUME_ROWS = "ROWS";

	/** row limit check (since 7.2) */
	public static final String TYPE_ROW_LIMIT = "row-limit";
	public static final String VALUE_ROW_LIMIT_EXCEEDED = "exceeded";
	public static final String ARG_ROW_LIMIT_CHECK = "check";
	public static final String ARG_ROW_LIMIT_DOWNSAMPLED = "downsampled";
	public static final String ARG_ROW_LIMIT_ABORTED = "aborted";
	public static final String VALUE_ROW_LIMIT_UPGRADE_FIX = "upgrade_fix";
	public static final String VALUE_ROW_LIMIT_UPGRADE_NOT_ENOUGH = "upgrade_not_enough";
	public static final String VALUE_ROW_LIMIT_UPGRADE_SELECTED = "upgrade_selected";
	public static final String ARG_ROW_LIMIT_NO_UPGRADE = "no_upgrade";

	/** commercial and educational sign up (since 7.3) */
	public static final String TYPE_SIGN_UP = "sign_up";
	public static final String VALUE_ACCOUNT_TYPE = "account_type";
	public static final String ARG_COMMERCIAL = "commercial";
	public static final String ARG_EDUCATIONAL = "educational";
	public static final String VALUE_ACCOUNT_CREATION = "account_creation";
	public static final String ARG_ACCOUNT_CREATION_ABORTED = "aborted";
	public static final String ARG_ACCOUNT_CREATION_SUCCESS = "success";
	public static final String ARG_ACCOUNT_ALREADY_EXISTS = "already_exists";
	public static final String ARG_COMMUNICATION_ERROR = "communication_error";
	public static final String VALUE_EMAIL_VERIFICATION = "email_verification";
	public static final String ARG_EMAIL_VERIFICATION_SUCCESS = "success";
	public static final String ARG_EMAIL_VERIFICATION_PENDING = "pending";

	/** row limit check additions (since 7.3) */
	public static final String VALUE_ROW_LIMIT_DIALOG = "dialog";

	/** beta features (since 7.3) */
	public static final String TYPE_BETA_FEATURES = "beta-features";
	public static final String VALUE_BETA_FEATURES_ACTIVATION = "activated";

	/** marketplace search (since 7.3) */
	public static final String TYPE_MARKETPLACE = "marketplace";
	public static final String VALUE_OPERATOR_SEARCH = "operator_search";
	public static final String VALUE_SEARCH = "search";
	public static final String VALUE_EXTENSION_INSTALLATION = "extension_installation";

	/** extension initialization (since 7.3) */
	public static final String VALUE_EXTENSION_INITIALIZATION = "extension_initialization";

	/** type cta (since 7.5) */
	public static final String TYPE_CTA = "cta";
	public static final String VALUE_CTA_FAILURE = "failure";
	public static final String VALUE_RULE_TRIGGERED = "cta_triggered";

	/** introduced in 8.0 */
	public static final String TYPE_RESOURCE_ACTION = "resource-action";
	public static final String TYPE_SIMPLE_ACTION = "simple-action";
	public static final String TYPE_PROCESS = "process";
	public static final String VALUE_EXECUTION = "execution";
	public static final String VALUE_EXCEPTION = "exception";
	public static final String VALUE_OPERATOR_COUNT = "operator_count";
	public static final String ARG_RUNTIME = "runtime";
	public static final String ARG_SUCCESS = "success";
	public static final String ARG_FAILED = "failed";
	public static final String ARG_STARTED = "started";
	public static final String ARG_STOPPED = "stopped";
	public static final String VALUE_CTA_LIMIT = "limit";
	public static final String ARG_CTA_LIMIT_DELETED_EVENTS = "deleted_events";
	public static final String ARG_CTA_LIMIT_DECREASED_TIMEFRAME = "decreased_timeframe";
	public static final String VALUE_MODE = "mode";
	public static final String VALUE_CONSTANT_START = "start";
	
	/** conversion constant for bytes to megabytes */
	private static final int BYTE_TO_MB = 1024 * 1024;

	private static final String ACTION_STATISTICS_TAG = "action-statistics";
	private static final String ACTION_TAG = "action";

	private static final boolean DISABLED = RapidMiner.getExecutionMode().isHeadless()
			&& RapidMiner.getExecutionMode() != RapidMiner.ExecutionMode.COMMAND_LINE;

	/**
	 * A Key defines an identifier that is used to store some collected usage data associated with it. It has 3 levels,
	 * TYPE, VALUE and ARG, where ARG may be null.
	 * A Key also determines (by the assigned AggregationIndicator) how a newly collected value will be combined into an
	 * existing aggregate.
	 * A Key can have multiple labels (LabelIndicators) associated to it these are there to add more details to
	 * aggregated value.
	 * The serialized form of a Key has only 3 parts due to legacy reasons, the type, the value and the ARG combined
	 * with indicators.
	 * The ARG combined with indicators part is serialized like this:
	 *   [ARG][label indicator 1][label indicator 2]..[label indicator n][aggregation indicator]
	 * For legacy reasons if the ARG is null and there is no label attached and aggregation indicator is SUM the
	 * serialized form of this part is null.
	 * For legacy reasons we use _ separator between the ARG and labels (actually _ is always part of the indicator name
	 * except for SUM, which is serialized as an empty string), but this implies that indicator names may clash with
	 * ARG, so choose ARG and names wisely.
	 */
	public static final class Key {

		/**
		 * An aggregation indicator defines a name a bifunction and an optional function.
		 * The name of the indicator will be shown in the XML serialized version of the key, the current implementation
		 * adds it as a postfix to the ARG part of the key.
		 * The bifunction defines how to merge a new value to an aggregated value or how to merge aggregated values.
		 * The optional function defines how to transform a value before it gets merged to the aggregated value.
		 */
		private enum AggregationIndicator {

			SUM("", Long::sum),
			MIN("_MIN", Long::min),
			MAX("_MAX", Long::max),

			/**
			 * In case of COUNT it does not matter what we want to aggregate, only the number of times we call aggregate
			 * matters
			 */
			COUNT("_COUNT", Long::sum, i -> 1L);

			/**
			 * Parses the serialized form of last part of a key and returns the remaining part and the aggregation
			 * indicator as a pair.
			 * This helper method supposes that argWithIndicators still contains the aggregation indicator.
			 *
			 * @param argWithIndicators
			 * @return
			 */
			private static Pair<AggregationIndicator, String> fromArgWithIndicators(String argWithIndicators) {
				if (argWithIndicators == null) {
					return new Pair<>(SUM, null);
				} else {
					for (AggregationIndicator aggregationIndicator : EnumSet.complementOf(EnumSet.of(SUM))) {
						if (argWithIndicators.endsWith(aggregationIndicator.toString())) {
							return new Pair<>(aggregationIndicator,
									argWithIndicators.substring(0, argWithIndicators.length() - aggregationIndicator.toString().length()));
						}
					}
					return new Pair<>(SUM, argWithIndicators);
				}
			}

			private final String name;
			private final BiFunction<Long, Long, Long> dataCombiner;
			private final Function<Long, Long> dataTransformer;

			AggregationIndicator(String name, BiFunction<Long, Long, Long> dataCombiner) {
				this(name, dataCombiner, null);
			}

			AggregationIndicator(String name, BiFunction<Long, Long, Long> dataCombiner,
					Function<Long, Long> dataTransformer) {
				this.name = name;
				this.dataCombiner = dataCombiner;
				this.dataTransformer = dataTransformer;
			}


			private Long transformData(Long data) {
				return dataTransformer != null ? dataTransformer.apply(data) : data;
			}

			@Override
			public String toString() { return name; }

		}

		/**
		 * A label indicator adds some details to a key.
		 */
		private enum LabelIndicator {

			// We use the default ordering of an EnumSet during serialization and parsing so never change ordering of
			// existing labels, but you can insert a new label to anywhere.

			/**
			 * We use this label to indicate timers that haven't been stopped (ie. uploaded usagestat.xml will contain
			 * information about the running processes)
			 */
			INCOMPLETE("_INCOMPLETE"),

			/**
			 * We use this label to indicate timers that were not stopped at all (ie. if we find the INCOMPLETE label
			 * during load from the usagestat.xml we change it to UNTERMINATED, and this is a bad sign).
			 */
			UNTERMINATED("_UNTERMINATED");

			/**
			 * Parses the serialized form of last part of a key and returns the remaining part and the set of label
			 * indicators as a pair.
			 * This helper method supposes that aggregation indicator has been removed from argWithIndicators and only
			 * ARG and label indicators need to be parsed.
			 *
			 * @param argWithIndicators
			 * @return
			 */
			private static Pair<Set<LabelIndicator>, String> fromArgWithIndicators(String argWithIndicators) {
				if (argWithIndicators == null) {
					return new Pair<>(EnumSet.noneOf(LabelIndicator.class), null);
				} else {
					Set<LabelIndicator> labelIndicators = EnumSet.noneOf(LabelIndicator.class);
					LabelIndicator[] lis = LabelIndicator.values();
					for (int i = lis.length - 1; i >= 0; -- i) {
						if (argWithIndicators.endsWith(lis[i].toString())) {
							labelIndicators.add(lis[i]);
							argWithIndicators = argWithIndicators.substring(0, argWithIndicators.length() - lis[i].toString().length());
						}
					}
					return new Pair<>(labelIndicators, argWithIndicators);
				}
			}

			private String name;

			LabelIndicator(String name) {
				this.name = name;
			}

			@Override
			public String toString() { return name; }

		}

		/**
		 * Returns a key from its string representation.
		 *
		 * @param type
		 * @param value
		 * @param argWithIndicators
		 * @return
		 */
		static Key fromArgWithIndicators(String type, String value, String argWithIndicators) {
			Pair<AggregationIndicator, String> p = AggregationIndicator.fromArgWithIndicators(argWithIndicators);
			Pair<Set<LabelIndicator>, String> p2 = LabelIndicator.fromArgWithIndicators(p.getSecond());

			return new Key(type, value, p2.getSecond(), p2.getFirst(), p.getFirst());
		}

		private final String type;
		private final String value;
		private final String arg;
		private final Set<LabelIndicator> labelIndicators;
		private final AggregationIndicator aggregationIndicator;
		private final String argWithIndicators;

		public Key(String type, String value, String arg) {
			this(type, value, arg, null, null);
		}

		public Key(String type, String value, String arg, AggregationIndicator aggregationIndicator) {
			this(type, value, arg, null, aggregationIndicator);
		}

		public Key(String type, String value, String arg, Set<LabelIndicator> labelIndicators) {
			this(type, value, arg, labelIndicators, null);
		}

		public Key(String type, String value, String arg, Set<LabelIndicator> labelIndicators,
				AggregationIndicator aggregationIndicator) {
			this.type = type;
			this.value = value;
			this.arg = arg;
			this.labelIndicators = labelIndicators == null ? EnumSet.noneOf(LabelIndicator.class) : labelIndicators;
			this.aggregationIndicator = aggregationIndicator == null ? AggregationIndicator.SUM: aggregationIndicator;
			argWithIndicators = computeArgWithIndicators();
		}

		private String computeArgWithIndicators() {
			if (arg == null && labelIndicators.isEmpty() && aggregationIndicator == AggregationIndicator.SUM) {
				return null;
			} else {
				String a = arg == null ? "" : arg;
				String lis = labelIndicators.stream().map(LabelIndicator::toString).collect(Collectors.joining(""));
				String ai = aggregationIndicator.toString();
				return a + lis + ai;
			}
		}

		/**
		 * Return the type component of the key.
		 *
		 * @return
		 */
		public String getType() {
			return type;
		}

		/**
		 * Return the value component of the key.
		 *
		 * @return
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Returns the string representation of ARG and indicators.
		 *
		 * @return
		 */
		public String getArgWithIndicators() {
			return argWithIndicators;
		}

		/**
		 * Sets the aggregation indicator of a key.
		 *
		 * @param ai
		 * @return
		 */
		private Key withAggregation(AggregationIndicator ai) {
			if (aggregationIndicator == ai) {
				return this;
			} else {
				return new Key(type, value, arg, labelIndicators, ai);
			}
		}

		/**
		 * Tests the aggregation indicator of a key.
		 *
		 * @param ai
		 * @return
		 */
		private boolean isAggregatedWith(AggregationIndicator ai) {
			return aggregationIndicator == ai;
		}

		/**
		 * Adds a label to a key.
		 *
		 * @param labelIndicator
		 * @return
		 */
		private Key withLabel(LabelIndicator labelIndicator) {
			if (labelIndicators.contains(labelIndicator)) {
				return this;
			} else {
				Set<LabelIndicator> lis = EnumSet.copyOf(labelIndicators);
				lis.add(labelIndicator);
				return new Key(type, value, arg, lis, aggregationIndicator);
			}
		}

		/**
		 * Removes a label from a key.
		 *
		 * @param labelIndicator
		 * @return
		 */
		private Key withoutLabel(LabelIndicator labelIndicator) {
			if (!labelIndicators.contains(labelIndicator)) {
				return this;
			} else {
				Set<LabelIndicator> lis = EnumSet.copyOf(labelIndicators);
				lis.remove(labelIndicator);
				return new Key(type, value, arg, lis, aggregationIndicator);
			}
		}

		/**
		 * Changes a label if it is attached.
		 *
		 * @param from
		 * @param to
		 * @return
		 */
		private Key withLabelChange(LabelIndicator from, LabelIndicator to) {
			if (!labelIndicators.contains(from)) {
				return this;
			} else {
				Set<LabelIndicator> lis = EnumSet.copyOf(labelIndicators);
				lis.remove(from);
				lis.add(to);
				return new Key(type, value, arg, lis, aggregationIndicator);
			}
		}

		/**
		 * Test the presence of a label on a key.
		 *
		 * @param labelIndicator
		 * @return
		 */
		private boolean isLabeledWith(LabelIndicator labelIndicator) {
			return labelIndicators.contains(labelIndicator);
		}

		/**
		 * A merges a new value to the aggregate.
		 *
		 * @param aggregatedData
		 * @param data
		 * @return
		 */
		private Long mergeData(Long aggregatedData, Long data) {
			if (aggregatedData == null) {
				return aggregationIndicator.transformData(data);
			} else if (data == null) {
				return aggregatedData;
			} else {
				return aggregationIndicator.dataCombiner.apply(aggregatedData, aggregationIndicator.transformData(data));
			}
		}

		/**
		 * Merges a new value to the aggregated statistics.
		 *
		 * @param statistics
		 * @param data
		 */
		private void mergeDataTo(Map<Key, Long> statistics, Long data) {
			if (data != null) {
				statistics.merge(this, aggregationIndicator.transformData(data), aggregationIndicator.dataCombiner);
			}
		}

		/**
		 * Merges an aggregated value to the aggregated statistics.
		 *
		 * @param statistics
		 * @param aggregatedData
		 */
		private void mergeAggregatedDataTo(Map<Key, Long> statistics, Long aggregatedData) {
			if (aggregatedData != null) {
				statistics.merge(this, aggregatedData, aggregationIndicator.dataCombiner);
			}
		}

		@Override
		public String toString() {
			return type + ",\t" + value + ",\t" + argWithIndicators;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Key key = (Key) o;

			if (type != null ? !type.equals(key.type) : key.type != null) {
				return false;
			}
			if (value != null ? !value.equals(key.value) : key.value != null) {
				return false;
			}
			return argWithIndicators != null ? argWithIndicators.equals(key.argWithIndicators) : key.argWithIndicators == null;
		}

		@Override
		public int hashCode() {
			int result = type != null ? type.hashCode() : 0;
			result = 31 * result + (value != null ? value.hashCode() : 0);
			result = 31 * result + (argWithIndicators != null ? argWithIndicators.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Transforms an exception stacktrace into a String
	 *
	 * @param e
	 * @return
	 */
	public static String getExceptionStackTraceAsString(Exception e) {
		return Stream.of(e.getStackTrace()).limit(40).map(StackTraceElement::toString).collect(Collectors.joining(","));
	}

	/** Listener that logs input and output volume at operator ports. */
	private final ProcessListener operatorVolumeListener = new ProcessListener() {

		@Override
		public void processStarts(Process process) {
			// not needed
		}

		@Override
		public void processStartedOperator(Process process, Operator op) {
			// log the input volumes of the operator
			for (InputPort inputPort : op.getInputPorts().getAllPorts()) {
				try {
					IOObject ioObject = inputPort.getDataOrNull(IOObject.class);
					if (ioObject instanceof ExampleSet) {
						ExampleSet exampleSet = (ExampleSet) ioObject;
						logInputVolume(op, inputPort, exampleSet.size(), exampleSet.getAttributes().allSize());
					}
				} catch (UserError e) {
					// cannot log volume
				}
			}
		}

		@Override
		public void processFinishedOperator(Process process, Operator op) {
			// log the output volumes of the operator
			for (OutputPort outputPort : op.getOutputPorts().getAllPorts()) {
				try {
					IOObject ioObject = outputPort.getDataOrNull(IOObject.class);
					if (ioObject instanceof ExampleSet) {
						ExampleSet exampleSet = (ExampleSet) ioObject;
						logOutputVolume(op, outputPort, exampleSet.size(), exampleSet.getAttributes().allSize());
					}
				} catch (UserError e) {
					// cannot log volume
				}

			}
			// log the memory volume used
			logMemory();
		}

		@Override
		public void processEnded(Process process) {
			// not needed
		}
	};

	/** Contains the aggregated usage statistics */
	private Map<Key, Long> statistics = new HashMap<>();
	private final Object statisticsLock = new Object();
	private Date lastReset;

	/** flag whether the rowLimit was already exceeded during this session */
	private boolean rowLimitExceeded;

	/** the last time when a user action was observed */
	private volatile long lastUserActionTime = -1;

	/**
	 * Singleton instance of ActionStatisticsCollector
	 *
	 * @return
	 */
	public static ActionStatisticsCollector getInstance() {
		return INSTANCE;
	}

	protected void start() {
		if (RapidMiner.getExecutionMode().isHeadless()) {
			return;
		}

		long eventMask = AWTEvent.MOUSE_EVENT_MASK + AWTEvent.KEY_EVENT_MASK;
		Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
			if (e.getID() == KeyEvent.KEY_RELEASED) {
				if (((KeyEvent) e).getComponent() != null) {
					lastUserActionTime = System.currentTimeMillis();
				}
			} else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
				final MouseEvent me = (MouseEvent) e;
				Component component = me.getComponent();
				logAction(component);
			}
		}, eventMask);

		RapidMinerGUI.getMainFrame().getDockingDesktop().addDockableStateChangeListener(e ->
				log(TYPE_DOCKABLE, e.getNewState().getDockable().getDockKey().getKey(),
				e.getNewState().getLocation().toString())
		);
	}

	/**
	 * Logs an {@link ActionEvent} to statistics, with the detailed circumstances of the event.
	 * 
	 * @param action
	 * @param actionEvent
	 */
	public void logAction(Action action, ActionEvent actionEvent) {
		String actionCommand = actionEvent.getActionCommand();
		if (actionCommand != null) {
			Object source = actionEvent.getSource();
			StringBuilder arg = new StringBuilder(source.getClass().getName());
			arg.append("|");
			arg.append(actionCommand);
			if (source instanceof AbstractButton) {
				arg.append("|");
				arg.append(((AbstractButton) source).getText());
				arg.append("|");
				arg.append(((AbstractButton) source).isSelected());
			} else if (source instanceof JTextComponent && !(source instanceof JPasswordField)) {
				arg.append("|");
				arg.append(((JTextComponent) source).getText());
			}
			if (action instanceof ResourceAction) {
				log(TYPE_RESOURCE_ACTION, ((ResourceAction) action).getKey(), arg.toString());
			} else {
				log(TYPE_SIMPLE_ACTION, (String) action.getValue(Action.NAME), arg.toString());
			}
		}
	}

	private void logAction(Object component) {
		if (component == null) {
			return;
		}
		lastUserActionTime = System.currentTimeMillis();
		if (component instanceof AbstractButton) {
			AbstractButton button = (AbstractButton) component;
			Action action = button.getAction();
			// Only log ResourceActions. Otherwise, we would also log recent files, including file
			// names, etc.
			if (action instanceof ResourceAction) {
				String actionCommand = button.getActionCommand();
				if (actionCommand != null) {
					if (button instanceof JToggleButton) {
						log(TYPE_ACTION, actionCommand, button.isSelected() ? "deselected" : "selected");
					} else {
						log(TYPE_ACTION, actionCommand, "clicked");
					}
				}
			}
		} else if (component instanceof AbstractLinkButton) {
			AbstractLinkButton button = (AbstractLinkButton) component;
			Action action = button.getAction();
			// Only log ResourceActions
			if (action instanceof ResourceAction) {
				log(TYPE_ACTION, ((ResourceAction) action).getKey(), "clicked");
			}
		}
	}

	/**
	 * Logs the operator execution event and adds the {@link ProcessListener} logging the operator
	 * volumes.
	 *
	 * @param process
	 *            the started process
	 */
	public void logExecution(Process process) {
		if (process == null) {
			return;
		}
		// add listener for operator port volume logging
		process.getRootOperator().addProcessListener(operatorVolumeListener);
		List<Operator> allInnerOperators = process.getRootOperator().getAllInnerOperators();
		int size = 0;
		for (Operator op : allInnerOperators) {
			if (op.isEnabled()) {
				log(TYPE_OPERATOR, op.getOperatorDescription().getKey(), OPERATOR_EVENT_EXECUTION);
				++ size;
			}
		}
		log(TYPE_PROCESS, VALUE_OPERATOR_COUNT, Integer.toString(size));
		startTimer(process, TYPE_PROCESS, VALUE_EXECUTION, ARG_RUNTIME);
	}

	/**
	 * Logs process execution success
	 */
	public void logExecutionSuccess() {
		log(TYPE_PROCESS, VALUE_EXECUTION, ARG_SUCCESS);
	}

	/**
	 * Logs process execution start
	 */
	public void logExecutionStarted() {
		log(TYPE_PROCESS, VALUE_EXECUTION, ARG_STARTED);
	}

	/**
	 * Logs the details of the exception thrown during process execution.
	 * The argument holds all information about the exception, i.e. related operator,
	 * error name, stacktrace.
	 * 
	 * @param process
	 *            The process being executed when the exception was thrown.
	 * @param e
	 *             The exception to be logged.
	 */
	public void logExecutionException(Process process, Exception e) {
		if (e instanceof ProcessStoppedException) {
			log(TYPE_PROCESS, VALUE_EXECUTION, ARG_STOPPED);
 		} else {
			log(TYPE_PROCESS, VALUE_EXECUTION, ARG_FAILED);
			StringBuilder exception = new StringBuilder(process.getCurrentOperator().getOperatorDescription().getKey());
			if (e instanceof UserError) {
				UserError ue = (UserError) e;
				exception.append("|ue|");
				exception.append(ue.getErrorName());
				exception.append("|");
				exception.append(ue.getOperator().getOperatorDescription().getKey());
				exception.append("|");
			} else {
				exception.append("|ex|");
				exception.append(e.toString());
				exception.append("||");
			}
			exception.append(getExceptionStackTraceAsString(e));
			log(TYPE_PROCESS, VALUE_EXCEPTION, exception.toString());
		}
	}

	/**
	 * Logs the execution time for all operators in the process and removes the
	 * {@link ProcessListener} logging the operator volumes.
	 *
	 * @param process
	 *            the finished process
	 */
	public void logExecutionFinished(Process process) {
		if (process == null) {
			return;
		}
		stopTimer(process);
		// remove listener for operator port volume logging
		process.getRootOperator().removeProcessListener(operatorVolumeListener);
		Collection<Operator> allInnerOperators = process.getAllOperators();
		for (Operator op : allInnerOperators) {
			// only log if the operator finished
			if (!op.isDirty()) {
				// retrieve execution time stored with the operator
				double executionTime = (double) op.getValue("execution-time").getValue();
				logOperatorExecutionTime(op, (long) executionTime);
			}
		}
	}

	/**
	 * Logs that the user exceeded the row limit and schedules a transmission soon.
	 */
	public void logRowLimitExceeded() {
		log(ActionStatisticsCollector.TYPE_ROW_LIMIT, ActionStatisticsCollector.VALUE_ROW_LIMIT_EXCEEDED,
				ActionStatisticsCollector.ARG_ROW_LIMIT_CHECK);
		if (!rowLimitExceeded) {
			rowLimitExceeded = true;
			UsageStatsScheduler.scheduleTransmission(UsageStatistics.Reason.ROWLIMIT, true);
		}
	}

	/**
	 * Logs that a CTA rule is triggered
	 *
	 * @param ruleID
	 * @param result
	 */
	public void logCtaRuleTriggered(String ruleID, String result) {
		log(ActionStatisticsCollector.VALUE_RULE_TRIGGERED, ruleID, result);
		UsageStatsScheduler.scheduleTransmission(UsageStatistics.Reason.CTA, true);
	}

	/**
	 * Logs the volume for the operator input port. Logs the columns, rows and cells (rows *
	 * columns) and for each their sum, min, max and count.
	 *
	 * @param operator
	 *            the operator the input port belongs to
	 * @param port
	 *            the input port for which to log the volume
	 * @param rows
	 *            the rows of the example set at the port
	 * @param columns
	 *            the columns of the example set at the port
	 */
	private void logInputVolume(Operator operator, InputPort port, int rows, int columns) {
		logVolume(TYPE_INPUT_VOLUME, operator, port, rows, columns);
	}

	/**
	 * Logs the volume for the operator output port. Logs the columns, rows and cells (rows *
	 * columns) and for each their sum, min, max and count.
	 *
	 * @param operator
	 *            the operator the output port belongs to
	 * @param port
	 *            the output port for which to log the volume
	 * @param rows
	 *            the rows of the example set at the port
	 * @param columns
	 *            the columns of the example set at the port
	 */
	private void logOutputVolume(Operator operator, OutputPort port, int rows, int columns) {
		logVolume(TYPE_OUTPUT_VOLUME, operator, port, rows, columns);
	}

	public void log(Operator op, String event) {
		if (op == null) {
			return;
		}
		log(TYPE_OPERATOR, op.getOperatorDescription().getKey(), event);
	}

	/** Adds 1 to the aggregated value */
	public void log(String type, String value, String arg) {
		log(new Key(type, value, arg), 1);
	}

	/**
	 * Logs the executionTime for the operator. Adjusts the sum, min, max and count of the execution
	 * times logged before.
	 *
	 * @param operator
	 *            the operator to log
	 * @param executionTime
	 *            the execution time (in milliseconds) to log
	 */
	private void logOperatorExecutionTime(Operator operator, long executionTime) {
		logCountSumMinMax(TYPE_OPERATOR, operator.getOperatorDescription().getKey(), OPERATOR_RUNTIME, executionTime);
	}

	/**
	 * Logs sum, max and count of the total memory currently used.
	 */
	private void logMemory() {
		long totalSize = Runtime.getRuntime().totalMemory() / BYTE_TO_MB;
		Key key = new Key(TYPE_MEMORY, MEMORY_USED, MEMORY_ARG);
		log(key, totalSize);
		logMax(key, totalSize);
		logCount(key, totalSize);
	}

	/**
	 * Logs the volume for an operator port. Logs the columns, rows and cells and for each their
	 * sum, min, max and count.
	 */
	private void logVolume(String type, Operator operator, Port port, int rows, int columns) {
		String value = operator.getOperatorDescription().getKey() + "." + port.getName();
		logCountSumMinMax(type, value, VOLUME_ROWS, rows);
		logCountSumMinMax(type, value, VOLUME_COLUMNS, columns);
		logCountSumMinMax(type, value, VOLUMNE_CELLS, (long) columns * rows);
	}

	/**
	 * For the key given by TYPE, VALUE and ARG logs the amount, its minimum and maximum and how
	 * often a amount was logged.
	 */
	void logCountSumMinMax(String type, String value, String arg, long data) {
		Key key = new Key(type, value, arg);
		log(key, data);
		logMin(key, data);
		logMax(key, data);
		logCount(key, data);
	}

	/**
	 * For the key given by type, value and arg logs the minimum and maximum amount.
	 */
	void logMinMax(String type, String value, String arg, long data) {
		Key key = new Key(type, value, arg);
		logMin(key, data);
		logMax(key, data);
	}

	private void log(Key key, long data) {
		if (DISABLED) {
			return;
		}

		synchronized (statisticsLock) {
			key.mergeDataTo(statistics, data);
		}
		if (key.isAggregatedWith(Key.AggregationIndicator.SUM) || key.isAggregatedWith(Key.AggregationIndicator.COUNT)) {
			CtaEventAggregator.INSTANCE.log(key, data);
		}
	}

	/**
	 * Logs the minimum amount that was logged for (TYPE, VALUE, ARG) under (TYPE, VALUE, ARG_MIN).
	 */
	private void logMin(Key key, long data) {
		log(key.withAggregation(Key.AggregationIndicator.MIN), data);
	}

	/**
	 * Logs the maximum amount that was logged for (TYPE, VALUE, ARG) under (TYPE, VALUE, ARG_MAX).
	 */
	private void logMax(Key key, long data) {
		log(key.withAggregation(Key.AggregationIndicator.MAX), data);
	}

	/**
	 * Logs the number of log executions for (TYPE, VALUE, ARG) under (TYPE, VALUE, ARG_COUNT).
	 */
	private void logCount(Key key, long data) {
		log(key.withAggregation(Key.AggregationIndicator.COUNT), data);
	}

	/**
	 * Running timers are attached to objects since 8.0 to be able to track concurrent items with a common key.
	 */
	private final IdentityHashMap<Object, Pair<Key, Long>> runningTimers = new IdentityHashMap<>();

	/**
	 * To remain compatible with pre 8.0 versions and offer startTimer and stopTimer with a key we track some
	 * internally generated objects if needed.
	 */
	private Map<Key, Object> runningTimerIds = new HashMap<>();

	/**
	 * Starts a timer based on key. There can be only one timer running with a particular key at a
	 * time.
	 * We have this method to remain compatible with pre 8.0 versions.
	 *
	 * @param type
	 * @param value
	 * @param arg
	 */
	public void startTimer(String type, String value, String arg) {
		if (DISABLED) {
			return;
		}

		Key key = new Key(type, value, arg);
		synchronized (runningTimers) {
			Object id = runningTimerIds.get(key);
			if (id == null) {
				id = new Object();
				runningTimerIds.put(key, id);
				runningTimers.put(id, new Pair<>(key, System.currentTimeMillis()));
			}
		}
	}

	/**
	 * Starts a timer with an id and a key. There there can be multiple timers running with the same
	 * key at a time as long as they have different ids.
	 *
	 * @param id
	 * @param type
	 * @param value
	 * @param arg
	 */
	public void startTimer(Object id, String type, String value, String arg) {
		if (DISABLED) {
			return;
		}

		Key key = new Key(type, value, arg);
		synchronized (runningTimers) {
			runningTimers.putIfAbsent(id, new Pair<>(key, System.currentTimeMillis()));
		}
	}

	/**
	 * Stops a timer with a key.
	 * We have this method to remain compatible with pre 8.0 versions.
	 *
	 * @param type
	 * @param value
	 * @param arg
	 */
	public void stopTimer(String type, String value, String arg) {
		if (DISABLED) {
			return;
		}

		Key key = new Key(type, value, arg);
		synchronized (runningTimers) {
			Object id = runningTimerIds.remove(key);
			if (id != null) {
				Pair<Key, Long> keyAndStartTime = runningTimers.remove(id);
				if (keyAndStartTime != null) {
					long time = System.currentTimeMillis() - keyAndStartTime.getSecond();
					log(key, time);
					logMin(key, time);
					logMax(key, time);
					logCount(key, time);
				}
			}
		}
	}

	/**
	 * Stops a timer with an id.
	 *
	 * @param id
	 */
	public void stopTimer(Object id) {
		if (DISABLED) {
			return;
		}

		synchronized (runningTimers) {
			Pair<Key, Long> keyAndStartTime = runningTimers.remove(id);
			if (keyAndStartTime != null) {
				Key key = keyAndStartTime.getFirst();
				long time = System.currentTimeMillis() - keyAndStartTime.getSecond();
				log(key, time);
				logMin(key, time);
				logMax(key, time);
				logCount(key, time);
			}
		}
	}

	/**
	 * Aggregate up the running timer stats.
	 *
	 * @return
	 */
	private Map<Key, Long> aggregateRunningTimers() {
		Map<Key, Long> incompleteStatistics = new HashMap<>();
		synchronized (runningTimers) {
			for (Entry<Object, Pair<Key, Long>> runningTimer : runningTimers.entrySet()) {
				Key key = runningTimer.getValue().getFirst().withLabel(Key.LabelIndicator.INCOMPLETE);
				long duration = System.currentTimeMillis() - runningTimer.getValue().getSecond();
				key.mergeDataTo(incompleteStatistics, duration);
				key.withAggregation(Key.AggregationIndicator.COUNT).mergeDataTo(incompleteStatistics, duration);
				key.withAggregation(Key.AggregationIndicator.MAX).mergeDataTo(incompleteStatistics, duration);
			}
		}

		return incompleteStatistics;
	}

	void init() {
		statistics.clear();
		this.lastReset = new Date();
	}

	void load(Element parent, Date lastReset) throws XMLException {
		Element actionStats = XMLTools.getChildElement(parent, ACTION_STATISTICS_TAG, false);
		if (actionStats != null) {
			synchronized (statisticsLock) {
				statistics.clear();
				this.lastReset = lastReset;
				NodeList actionElements = parent.getElementsByTagName(ACTION_TAG);
				for (int i = 0; i < actionElements.getLength(); i++) {
					Element actionElement = (Element) actionElements.item(i);
					Key key = Key.fromArgWithIndicators(XMLTools.getTagContents(actionElement, "type"),
							XMLTools.getTagContents(actionElement, "value"), XMLTools.getTagContents(actionElement, "arg"));
					long data = XMLTools.getTagContentsAsLong(actionElement, "count");
					key = key.withLabelChange(Key.LabelIndicator.INCOMPLETE, Key.LabelIndicator.UNTERMINATED);
					key.mergeAggregatedDataTo(statistics, data);
				}
			}
		}
	}

	/**
	 * This is a snapshot of the collected statistics.
	 * We present this snapshot to the user before uploading. If the upload doesn't succeed we merge this snapshot back
	 * to the statistics.
	 */
	public static class ActionStatisticsSnapshot {

		private final Map<Key, Long> statistics;
		private final Date from;
		private final Date to;

		ActionStatisticsSnapshot(Map<Key, Long> statistics, Date from) {
			this.statistics = statistics;
			this.from = from;
			to = new Date();
		}

		/**
		 * Return the map representation of the snapshot
		 *
		 * @return
		 */
		public Map<Key, Long> getStatistics() {
			return statistics;
		}

		/**
		 * Returns the date when actions in this snapshot are collected from.
		 *
		 * @return
		 */
		public Date getFrom() {
			return from;
		}

		/**
		 * Returns the date when actions in this snapshot are collected to.
		 *
		 * @return
		 */
		public Date getTo() {
			return to;
		}

		/**
		 * Addends the XML representation of this snapshot to a parent XML element.
		 *
		 * @param doc
		 * @param parent
		 */
		public void toXML(Document doc, Element parent) {
			Element actionStatisticsTag = doc.createElement(ACTION_STATISTICS_TAG);
			doc.getDocumentElement().appendChild(actionStatisticsTag);

			for (Map.Entry<ActionStatisticsCollector.Key, Long> stat : statistics.entrySet()) {
				Element actionTag = doc.createElement(ACTION_TAG);
				ActionStatisticsCollector.Key key = stat.getKey();
				Long value = stat.getValue();
				XMLTools.addTag(actionTag, "type", key.getType());
				XMLTools.addTag(actionTag, "value", key.getValue());
				String argWithIndicators = key.getArgWithIndicators();
				if (argWithIndicators != null) {
					XMLTools.addTag(actionTag, "arg", argWithIndicators);
				}
				XMLTools.addTag(actionTag, "count", String.valueOf(value));
				actionStatisticsTag.appendChild(actionTag);
			}

			actionStatisticsTag.setAttribute("os-name", System.getProperty("os.name"));
			actionStatisticsTag.setAttribute("os-version", System.getProperty("os.version"));

			parent.appendChild(actionStatisticsTag);
		}
	}

	/**
	 * Return the current snapshot of the collected statistics, removes the snapshot data from the current
	 * statistics if needed.
	 *
	 * @param remove
	 * @return
	 */
	public ActionStatisticsSnapshot getActionStatisticsSnapshot(boolean remove) {
		synchronized (runningTimers) {
			synchronized (statisticsLock) {
				Map<Key, Long> runningStats = aggregateRunningTimers();
				ActionStatisticsSnapshot snapshot;
				if (remove) {
					statistics.putAll(runningStats);
					snapshot = new ActionStatisticsSnapshot(statistics, lastReset);
					statistics = new HashMap<>();
					lastReset = new Date();
				} else {
					runningStats.putAll(statistics);
					snapshot = new ActionStatisticsSnapshot(runningStats, lastReset);
				}

				return snapshot;
			}
		}
	}

	/**
	 * Adds a snapshot to the current statistics.
	 *
	 * @param snapshot
	 */
	public void addActionStatisticsSnapshot(ActionStatisticsSnapshot snapshot) {
		synchronized (statisticsLock) {
			for (Entry<Key, Long> stat : snapshot.statistics.entrySet()) {
				Key key = stat.getKey();
				if (!key.isLabeledWith(Key.LabelIndicator.INCOMPLETE)) {
					key.mergeAggregatedDataTo(statistics, stat.getValue());
				}
			}
			if (lastReset == null || snapshot.getFrom().before(lastReset)) {
				lastReset = snapshot.getFrom();
			}
		}
	}

	/**
	 * Return the collected data that belongs to the key.
	 *
	 * @param type
	 * @param value
	 * @param arg
	 * @return
	 */
	public long getData(String type, String value, String arg) {
		Key key = new Key(type, value, arg);
		if (key.isLabeledWith(Key.LabelIndicator.INCOMPLETE)) {
			key = key.withoutLabel(Key.LabelIndicator.INCOMPLETE);
			synchronized (runningTimers) {
				Long data = null;
				for (Entry<Object, Pair<Key, Long>> runningTimer : runningTimers.entrySet()) {
					Key runningTimerKey = runningTimer.getValue().getFirst();
					if (runningTimerKey.equals(key)) {
						Long incompleteData = System.currentTimeMillis() - runningTimer.getValue().getSecond();
						data = key.mergeData(data, incompleteData);
					}
				}
				return data == null ? 0 : data;
			}
		} else {
			synchronized (statisticsLock) {
				return statistics.getOrDefault(key, 0L);
			}
		}
	}

	/**
	 * Returns the last time when a user initiated an action (ie. meaningful user interaction
	 * happened with studio)
	 *
	 * @return the time in millis
	 */
	public long getLastUserActionTime() {
		return lastUserActionTime;
	}

}
