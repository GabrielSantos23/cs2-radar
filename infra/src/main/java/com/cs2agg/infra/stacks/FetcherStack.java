package com.cs2agg.infra.stacks;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class FetcherStack extends Stack {
    private final Queue queue;
    private final Function fetcherLambda;

    public FetcherStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.queue = Queue.Builder.create(this, "MatchQueue")
                .queueName("cs2-match-queue")
                .visibilityTimeout(Duration.seconds(60))
                .build();

        Map<String, String> environment = new HashMap<>();
        environment.put("PANDASCORE_API_KEY", "{{resolve:ssm:/cs2agg/pandascore-key}}");
        environment.put("SQS_QUEUE_URL", queue.getQueueUrl());

        this.fetcherLambda = Function.Builder.create(this, "FetcherLambda")
                .runtime(Runtime.JAVA_21)
                .handler("com.cs2agg.fetcher.FetcherHandler")
                .code(Code.fromAsset("../fetcher/target/fetcher-1.0.0-SNAPSHOT-shaded.jar"))
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .environment(environment)
                .build();

        queue.grantSendMessages(fetcherLambda);

        Rule rule = Rule.Builder.create(this, "FetcherScheduleRule")
                .schedule(Schedule.rate(Duration.minutes(5)))
                .build();
        rule.addTarget(new LambdaFunction(fetcherLambda));
    }

    public Queue getQueue() {
        return queue;
    }
}
