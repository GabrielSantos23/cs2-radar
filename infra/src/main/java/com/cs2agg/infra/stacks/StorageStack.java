package com.cs2agg.infra.stacks;

import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.elasticache.CfnReplicationGroup;
import software.amazon.awscdk.services.elasticache.CfnSubnetGroup;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

import java.util.List;
import java.util.stream.Collectors;

public class StorageStack extends Stack {
    private final Table table;
    private final IVpc vpc;
    private final String redisHost;
    private final SecurityGroup redisSecurityGroup;

    public StorageStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.table = Table.Builder.create(this, "MatchesTable")
                .tableName("cs2-matches")
                .partitionKey(Attribute.builder().name("tournamentId").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("matchId").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .timeToLiveAttribute("ttl")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        this.vpc = Vpc.Builder.create(this, "Vpc")
                .maxAzs(2)
                .natGateways(1)
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                .cidrMask(24)
                                .build()
                ))
                .build();

        this.redisSecurityGroup = SecurityGroup.Builder.create(this, "RedisSecurityGroup")
                .vpc(vpc)
                .allowAllOutbound(true)
                .description("Allow TCP 6379 inbound for ElastiCache Redis")
                .build();

        this.redisSecurityGroup.addIngressRule(
                Peer.ipv4(vpc.getVpcCidrBlock()), 
                Port.tcp(6379), 
                "Allow Redis traffic from within VPC"
        );

        List<String> privateSubnetIds = vpc.getPrivateSubnets().stream()
                .map(ISubnet::getSubnetId)
                .collect(Collectors.toList());

        CfnSubnetGroup redisSubnetGroup = CfnSubnetGroup.Builder.create(this, "RedisSubnetGroup")
                .description("Subnets for ElastiCache Redis")
                .subnetIds(privateSubnetIds)
                .build();

        CfnReplicationGroup redisReplicationGroup = CfnReplicationGroup.Builder.create(this, "RedisCache")
                .replicationGroupDescription("CS2 Match Odds Cache")
                .cacheNodeType("cache.t3.micro")
                .engine("redis")
                .numCacheClusters(1)
                .automaticFailoverEnabled(false)
                .cacheSubnetGroupName(redisSubnetGroup.getRef())
                .securityGroupIds(List.of(redisSecurityGroup.getSecurityGroupId()))
                .build();

        this.redisHost = redisReplicationGroup.getAttrPrimaryEndPointAddress();
    }

    public Table getTable() {
        return table;
    }

    public IVpc getVpc() {
        return vpc;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public SecurityGroup getRedisSecurityGroup() {
        return redisSecurityGroup;
    }
}
