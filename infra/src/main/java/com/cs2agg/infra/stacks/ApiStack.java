package com.cs2agg.infra.stacks;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigatewayv2.CorsHttpMethod;
import software.amazon.awscdk.services.apigatewayv2.CorsPreflightOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpRouteIntegration;
import software.amazon.awscdk.services.apigatewayv2.HttpRouteIntegrationConfig;
import software.amazon.awscdk.services.apigatewayv2.HttpRouteIntegrationBindOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpIntegrationType;
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SnapStartConf;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiStack extends Stack {

    public ApiStack(
            final Construct scope, 
            final String id, 
            final StackProps props,
            final IVpc vpc,
            final Table table,
            final Table bracketsTable,
            final Table rankingTable,
            final String redisHost,
            final SecurityGroup redisSecurityGroup) {
        super(scope, id, props);

        SecurityGroup lambdaSecurityGroup = SecurityGroup.Builder.create(this, "ApiLambdaSecurityGroup")
                .vpc(vpc)
                .allowAllOutbound(true)
                .description("Security group for API Lambda")
                .build();

        Map<String, String> environment = new HashMap<>();
        environment.put("DYNAMODB_TABLE", table.getTableName());
        environment.put("REDIS_HOST", redisHost);

        Function apiLambda = Function.Builder.create(this, "ApiLambda")
                .runtime(Runtime.JAVA_21)
                .handler("com.cs2agg.api.ApiHandler")
                .code(Code.fromAsset("../api/target/api-1.0.0-SNAPSHOT-shaded.jar"))
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .environment(environment)
                .vpc(vpc)
                .securityGroups(List.of(lambdaSecurityGroup))
                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
                .build();

        table.grantReadData(apiLambda);
        bracketsTable.grantReadData(apiLambda);
        rankingTable.grantReadData(apiLambda);
        
        apiLambda.grantInvoke(new ServicePrincipal("apigateway.amazonaws.com"));

        HttpApi httpApi = HttpApi.Builder.create(this, "HttpApi")
                .apiName("CS2MatchAggregatorApi")
                .build();

        HttpRouteIntegration integration = new HttpRouteIntegration("ApiLambdaIntegration") {
            @Override
            public HttpRouteIntegrationConfig bind(final HttpRouteIntegrationBindOptions options) {
                return HttpRouteIntegrationConfig.builder()
                        .type(HttpIntegrationType.AWS_PROXY)
                        .uri(apiLambda.getFunctionArn())
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build();
            }
        };

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/matches/upcoming")
                .methods(List.of(HttpMethod.GET))
                .integration(integration)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/matches/{id}")
                .methods(List.of(HttpMethod.GET))
                .integration(integration)
                .build());
 
        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/tournaments")
                .methods(List.of(HttpMethod.GET))
                .integration(integration)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/teams/{id}/matches")
                .methods(List.of(HttpMethod.GET))
                .integration(integration)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/tournaments/{id}/bracket")
                .methods(List.of(HttpMethod.GET))
                .integration(integration)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/ranking")
                .methods(List.of(HttpMethod.GET))
                .integration(integration)
                .build());
    }
}
