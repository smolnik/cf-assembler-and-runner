package cloud.developing.cf;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;

import cloud.developing.core.Params;
import cloud.developing.core.Task;
import cloud.developing.core.Utils;

/**
 * @author asmolnik
 *
 */
public class DbTask implements Task {

	@Override
	public Params execute(Params params) {
		String template = Utils.readTemplateFile("mysql.yml");
		String envName = "prod-us-1";
		AmazonCloudFormation cf = Utils.getCF(params.getValue("region"));
		cf.createStack(new CreateStackRequest().withParameters(params.getCFParams()).withStackName("db-" + envName)
				.withTags(Utils.getProdTags(envName)).withTemplateBody(template));
		return new Params();
	}

	public static void main(String[] args) {
		Params params = new Params().with("region", Regions.US_EAST_1.getName()).with("envTypeValue", "prod").with("masterPassword", "xyz")
				.with("dbSecurityGroup", "sg-b0a18dca").with("dbSubnet1", "subnet-d06edf99").with("dbSubnet2", "subnet-53a06408");
		new DbTask().execute(params);
	}

}
