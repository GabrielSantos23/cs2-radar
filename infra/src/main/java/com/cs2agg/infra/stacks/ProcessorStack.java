package com.cs2agg.infra.stacks;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class ProcessorStack extends Stack {

    public ProcessorStack(
            final Construct scope, 
            final String id, 
            final StackProps props,
            final IVpc vpc,
            final Table table,
            final Table bracketsTable,
            final Queue queue,
            final String redisHost,
            final SecurityGroup redisSecurityGroup) {
        super(scope, id, props);

        Cluster cluster = Cluster.Builder.create(this, "ProcessorCluster")
                .vpc(vpc)
                .build();

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, "ProcessorTaskDef")
                .cpu(512)
                .memoryLimitMiB(1024)
                .build();

        Map<String, String> environment = new HashMap<>();
        environment.put("REDIS_HOST", redisHost);
        environment.put("SQS_QUEUE_URL", queue.getQueueUrl());
        environment.put("AWS_REGION", getRegion());

        taskDefinition.addContainer("ProcessorContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromAsset("../processor"))
                .environment(environment)
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder().streamPrefix("cs2-processor").build()))
                .build());

        FargateService service = FargateService.Builder.create(this, "ProcessorService")
                .cluster(cluster)
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .assignPublicIp(false)
                .build();

        queue.grantConsumeMessages(taskDefinition.getTaskRole());
        table.grantReadWriteData(taskDefinition.getTaskRole());
        bracketsTable.grantReadWriteData(taskDefinition.getTaskRole());
    }
}
