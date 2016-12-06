package cloud.developing.core;

import static cloud.developing.core.Constants.ENV_NAME_TAG_KEY;
import static cloud.developing.core.Constants.ENV_TYPE_TAG_KEY;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;

/**
 * @author asmolnik
 *
 */
public class Utils {

	public static final AmazonCloudFormationClient getCF(String region) {
		AmazonCloudFormationClient cf = new AmazonCloudFormationClient();
		cf.setRegion(Region.getRegion(Regions.fromName(region)));
		return cf;
	}

	public static final String readTemplateFile(String path) {
		try {
			return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static final Collection<Tag> getProdTags(String envName) {
		return Arrays.asList(new Tag().withKey("ach:env-name").withValue(envName), new Tag().withKey("ach:env-type").withValue("prod"));
	}

	public static final void walkEovInstances(String envNameValue, BiConsumer<Instance, AmazonEC2> consumer, AWSCredentialsProvider credentials,
			Region region) {
		AmazonEC2 ec2 = new AmazonEC2Client(credentials);
		ec2.setRegion(region);
		ec2.describeInstances(new DescribeInstancesRequest().withFilters(new Filter().withName("tag:" + ENV_NAME_TAG_KEY).withValues(envNameValue)))
				.getReservations().stream().flatMap(r -> r.getInstances().stream()).forEach(i -> {
					consumer.accept(i, ec2);
				});
	}

	public static final List<com.amazonaws.services.ec2.model.Tag> getEnvTags(String envNameValue, String envTypeValue) {
		List<com.amazonaws.services.ec2.model.Tag> tags = new ArrayList<>();
		tags.add(new com.amazonaws.services.ec2.model.Tag(ENV_NAME_TAG_KEY, envNameValue));
		tags.add(new com.amazonaws.services.ec2.model.Tag(ENV_TYPE_TAG_KEY, envTypeValue));
		return tags;
	}

	public static final String findFirstEc2TagValue(String tagKey, Collection<com.amazonaws.services.ec2.model.Tag> tags) {
		return tags.stream().filter(t -> tagKey.equals(t.getKey())).map(t -> t.getValue()).findFirst().get();
	}

	public static final Map<String, String> waitForStack(String stackId, AmazonCloudFormationClient cf) {
		String status = getStatus(stackId, cf);
		log(stackId, status, 0);
		LocalDateTime then = LocalDateTime.now();
		while (StackStatus.CREATE_IN_PROGRESS.toString().equals(status)) {
			try {
				TimeUnit.SECONDS.sleep(15);
				status = getStatus(stackId, cf);
				log(stackId, status, ChronoUnit.MINUTES.between(then, LocalDateTime.now()));
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
		return getStack(stackId, cf).getOutputs().stream().collect(Collectors.toMap(o -> o.getOutputKey(), o -> o.getOutputValue()));
	}

	private static final void log(String stackId, String status, long timeElapsedInMin) {
		System.out.println("Time elapsed(min): " + timeElapsedInMin + ", stack status " + stackId.split("/")[1] + ": " + status + " timestamp: "
				+ LocalDateTime.now());
	}

	private static final String getStatus(String stackId, AmazonCloudFormationClient cf) {
		return getStack(stackId, cf).getStackStatus();
	}

	private static final Stack getStack(String stackId, AmazonCloudFormationClient cf) {
		return cf.describeStacks(new DescribeStacksRequest().withStackName(stackId)).getStacks().get(0);
	}

	private Utils() {

	}

}
