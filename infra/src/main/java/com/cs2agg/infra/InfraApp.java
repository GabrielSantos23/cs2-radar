package com.cs2agg.infra;

import com.cs2agg.infra.stacks.ApiStack;
import com.cs2agg.infra.stacks.FetcherStack;
import com.cs2agg.infra.stacks.ProcessorStack;
import com.cs2agg.infra.stacks.StorageStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        StackProps props = StackProps.builder().build();

        // 1. Storage Stack (DynamoDB + VPC + Redis)
        StorageStack storageStack = new StorageStack(app, "CS2StorageStack", props);

        // 2. Fetcher Stack (SQS + Fetcher Lambda + EventBridge)
        FetcherStack fetcherStack = new FetcherStack(app, "CS2FetcherStack", props);

        // 3. Processor Stack (ECS Fargate)
        new ProcessorStack(app, "CS2ProcessorStack", props,
                storageStack.getVpc(),
                storageStack.getTable(),
                storageStack.getBracketsTable(),
                storageStack.getRankingTable(),
                fetcherStack.getQueue(),
                storageStack.getRedisHost(),
                storageStack.getRedisSecurityGroup()
        );

        // 4. API Stack (API Gateway + API Lambda)
        new ApiStack(app, "CS2ApiStack", props,
                storageStack.getVpc(),
                storageStack.getTable(),
                storageStack.getBracketsTable(),
                storageStack.getRankingTable(),
                storageStack.getRedisHost(),
                storageStack.getRedisSecurityGroup()
        );

        app.synth();
    }
}
