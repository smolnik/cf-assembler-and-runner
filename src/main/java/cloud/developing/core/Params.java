package cloud.developing.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudformation.model.Parameter;

/**
 * @author asmolnik
 *
 */
public class Params {

	static class Param {

		final String key, value;

		private Param(String key, String value) {
			this.key = key;
			this.value = value;
		}

	}

	private List<Param> params = new ArrayList<>();

	public Params with(String key, String value) {
		params.add(new Param(key, value));
		return this;
	}

	public List<Param> getParams() {
		return params;
	}

	public List<Parameter> getCFParams() {
		return params.stream().map(p -> new Parameter().withParameterKey(p.key).withParameterValue(p.value)).collect(Collectors.toList());
	}

	public List<Parameter> getCFParamsExcept(String... exclusions) {
		return params.stream().filter(p -> Arrays.stream(exclusions).noneMatch(exclusion -> exclusion.equals(p.key)))
				.map(p -> new Parameter().withParameterKey(p.key).withParameterValue(p.value)).collect(Collectors.toList());
	}

	public String getValue(String key) {
		return params.stream().filter(p -> key.equals(p.key)).map(t -> t.value).findAny().get();
	}

}
