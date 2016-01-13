// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.Settings;
import opendial.Settings.CustomFunction;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

public class FunctionalTemplate implements Template {

	final CustomFunction function;

	final List<Template> parameters = new ArrayList<Template>();

	final Set<String> slots = new HashSet<String>();

	/**
	 * Creates a new string template.
	 * 
	 * @param string the string object
	 */
	protected FunctionalTemplate(String string) {
		string = string.trim();
		String funcName = string.substring(0, string.indexOf('('));
		function = Settings.getFunction(funcName);
		StringBuilder curParam = new StringBuilder();
		int openParams = 0;
		for (int i = funcName.length() + 1; i < string.length() - 1; i++) {
			char c = string.charAt(i);
			if (c == '(') {
				openParams++;
			}
			else if (c == ')') {
				openParams--;
			}
			else if (openParams == 0 && c == ',') {
				Template param = Template.create(curParam.toString());
				parameters.add(param);
				slots.addAll(param.getSlots());
				curParam = new StringBuilder();
				continue;
			}
			curParam.append(c);
		}
		Template param = Template.create(curParam.toString());
		parameters.add(param);
		slots.addAll(param.getSlots());
	}

	@Override
	public Set<String> getSlots() {
		return slots;
	}

	@Override
	public boolean isUnderspecified() {
		return !slots.isEmpty();
	}

	@Override
	public MatchResult match(String str) {
		if (isUnderspecified()) {
			StringTemplate st = new StringTemplate(fillSlots(new Assignment()));
			return st.match(str);
		}
		return new MatchResult(false);
	}

	@Override
	public List<MatchResult> find(String str, int maxResults) {
		if (isUnderspecified()) {
			StringTemplate st = new StringTemplate(fillSlots(new Assignment()));
			return st.find(str, maxResults);
		}
		return Collections.emptyList();
	}

	@Override
	public boolean isFilledBy(Assignment input) {
		return input.containsVars(slots);
	}

	@Override
	public String fillSlots(Assignment fillers) {
		return getValue(fillers).toString();
	}

	public Value getValue(Assignment fillers) {
		List<String> filledParams = parameters.stream()
				.map(p -> p.fillSlots(fillers)).collect(Collectors.toList());

		return function.apply(filledParams);
	}

	@Override
	public int hashCode() {
		return Math.abs(function.hashCode() + parameters.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(function.getName() + "(");
		String paramsStr = parameters.stream().map(p -> p.toString())
				.collect(Collectors.joining(","));
		builder.append(paramsStr + ")");
		return builder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FunctionalTemplate) {
			return o.toString().equals(toString());
		}
		return false;
	}

	public CustomFunction getFunction() {
		return function;
	}

}
