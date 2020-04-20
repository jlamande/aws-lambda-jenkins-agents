# λ Lambda Jenkins Agents

*Companion repository of the [AWS Lambda Cloud Plugin for Jenkins](https://plugins.jenkins.io/aws-lambda-cloud/)*

A Lambda Jenkins Agent is a Lambda function which connects to Jenkins as do slaves (nodes). Once it is connected and authenticated, Jenkins will pass to the Lambda function the different commands of your build (git, maven, ...).

So :
1. the lambda must include the [Jenkins Remoting Java Library](https://github.com/jenkinsci/remoting/blob/master/README.md#documentation) and use it to connect to Jenkins.
2. the lambda must include the binaries to execute commands. You'll see below how you can use Lambda Layers for that.

Depending on your needs you will have to deploy as much Lambda functions as necessary for :
- different capacities of Lambdas (adjusting memories)
- different tools (one with node, one with python, one with both, ...)

## TL;DR

### Deploy with AWS SAM
Requirements :
- java11
- maven
- [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- an already deployed SAM environment (S3 bucket and IAM role)

```
sam build
sam deploy --s3-bucket <your sam deployment bucket> --region <aws region>
```

### Deploy with serverless
Requirements :
- java11
- maven
- node.js/npm

```
cd lambda
mvn package
cd ..
npm install -g serverless
sls deploy
```

## Adapt the sources

Depending on your Jenkins version, the remoting Library might not work. So adapt the dependency version in the `agent/lambda/pom.xml` to match your Jenkins version.

## Deploy it

There are many different ways of deploying Lambdas (AWS SAM, Cloudformation, CDK, Serverless framework, ...). You'll find in this repository :
- a `template.yaml` file (and `samconfig.toml`) for deployment with [AWS SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/)
- a `serverless.yml` file for deployment with [Serverless framework](https://serverless.com/framework/docs/)

### Deploy with SAM

#### Build and Package it

Execute :
```
sam build
```

SAM includes a maven build when using the `sam build` command.

#### Deploy it

execute :
```
sam deploy --s3-bucket <your sam deployment bucket> --region <aws region>
```

Customize the `samconfig.toml` file if you want to avoid passing the 2 parameters and/or adapt to your environment (stack name, ...).

With this sample (look at the `serverless.yml` file), 2 Lambda functions are deployed into your account :

1. `jnlp-agent-git-bash` with the 2 prebuilt layers :
   - `arn:aws:lambda:${AWS::Region}:744348701589:layer:bash:8`
   - `arn:aws:lambda:${AWS::Region}:553035198032:layer:git-lambda2:4`
2. `jnlp-agent-git-bash-node` with the 3 prebuilt layers :
   - `arn:aws:lambda:${AWS::Region}:744348701589:layer:bash:8`
   - `arn:aws:lambda:${AWS::Region}:553035198032:layer:git-lambda2:4`
   - `arn:aws:lambda:${AWS::Region}:553035198032:layer:nodejs12:26`

### Deploy with the Serverless Framework

#### Build and Package it

First package the lambda (the [shade plugin](https://maven.apache.org/plugins/maven-shade-plugin/) is used to include runtime dependencies in the package) :

Go to the `lambda` folder and execute :
```
mvn package
```

*This code source is a work in progress but it is sufficient in order to be launched and connect to Jenkins.*

In `target` you fill find the agent jar required ot launch a Lambda connecting to Jenkins.

#### Deploy it

Execute :
```
npm install -g serverless
sls deploy
```

*It assumes that you have configured your AWS local environment. Typically as you would do for the [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html).*

With this sample (look at the `serverless.yml` file), 2 Lambda functions are deployed into your account :

1. `jnlp-agent-git-bash` with the 2 prebuilt layers :
   - `arn:aws:lambda:${self:provider.region}:744348701589:layer:bash:8`
   - `arn:aws:lambda:${self:provider.region}:553035198032:layer:git-lambda2:4`
2. `jnlp-agent-git-bash-node` with the 3 prebuilt layers :
   - `arn:aws:lambda:${self:provider.region}:744348701589:layer:bash:8`
   - `arn:aws:lambda:${self:provider.region}:553035198032:layer:git-lambda2:4`
   - `arn:aws:lambda:${self:provider.region}:553035198032:layer:nodejs12:26`

For advanced configurations, look at the [serverless documentaton](https://serverless.com/framework/docs/providers/aws/guide/intro/) :
- Network : if you require your Lambda to be deployed in a specific network (typically a specific VPC), configure the `vpc` part of the `serverless.yml` file.
- IAM : use IAM roles or `iamRoleStatements`
- ...

## Use the Lambda Function as an agent in Jenkins

Once your lambda(s) is(are) deployed, and if you have installed the `AWS Lambda Cloud` plugin, go the configuration page of Jenkins (and more recently to the dedicated Cloud Configuration page) to configure an **AWS Lambda Cloud** in Jenkins: 
- Set your AWS Credentials if required (not if your Jenkins is running on AWS EC2/ECS/EKS and its role allows to list and invoke Lambda functions)
- Declare a function and choose one of the freshly deployed ones
- Set a label for this function (example: `lambda-node`)
- Start using it in one of your pipelines as :
```groovy
pipeline {
    agent any
    stages {
        stage('test') {
            agent { label 'lambda-node'}
            steps {
                sh'''
                bash --version
                git --version
                node --version
                '''
            }
        }
    }
}
```

## More about Lambda Layers

You should customize the deployments of the sample Lambdas to add to them the required tools for your builds or deployments (git, aws cli, node, ...). You can choose to **include these tools directly into your Lambda packages** or to use **Lambda Layers**.

My recommended way is to use **Lambda Layers** :
> Lambda functions in a serverless application typically share common dependencies such as SDKs, frameworks, and now runtimes. With layers, you can centrally manage common components across multiple functions enabling better code reuse.

<https://docs.aws.amazon.com/lambda/latest/dg/configuration-layers.html#configuration-layers-path>

A good starting point on how to reuse layers or how to build your own layers : <https://github.com/mthenw/awesome-layers>

Example : **[how to add git in your Lambda as a layer](https://blog.enki.com/aws-layers-and-how-to-install-git-in-your-lambda-job-9701387ac538)**

### Limitations

- 5 layers maximum per Lambda Function. If you need more, you will have to combine and create your own ones
