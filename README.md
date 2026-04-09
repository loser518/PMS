# **A、登录与注册**（√）

系统提供了统一的登录界面，用户可选择以管理员、指导教师或学生的角色进行登录。对于首次使用系统的学生和指导教师，还提供了相应的注册功能。

# **B、学生管理**（√）

管理员和指导教师可以在该模块对学生信息进行管理，包括查看、添加、编辑和删除学生信息等操作，确保学生信息的准确性和完整性。

# **C、课题项目申报管理**（√）

学生可以通过系统进行课题项目的申报，填写课题项目的相关信息，如课题项目名称、课题项目类型等。提交申报后，等待指导教师和管理员的审核。

# **D、课题审核管理**（√）

学生视角：学生可以查看自己课题项目申请的状态和结果，也可以对课题项目申请信息进行一定的管理操作，如在未审核通过前修改信息等。

指导教师与管理员视角：指导教师和管理员在该模块对学生提交的课题项目申请进行审核，填写审核结果，决定是否通过课题申请。

# E、课题进度管理（√）

学生视角：学生可以上传课题项目的进度文件，更新课题项目的进展情况，方便指导教师和管理员及时了解课题动态。

指导教师与管理员视角：指导教师和管理员对学生上传的课题项目进度进行审核，给出审核意见，确保课题项目按照计划顺利推进。同时，还可以下载课题项目进度文件进行查看。

# F、公告信息管理（√）

管理员可以发布与课题项目相关的公告信息，如课题项目申报通知、审核结果公告、课题项目活动安排等。学生和指导教师可以在系统中查看公告信息，及时获取最新的课题项目动态和要求。

# G、个人中心（√）

各类用户可以在个人中心查看和修改自己的个人信息，如登录密码、联系方式等，以保证个人信息的安全性和准确性。

# 拉取并运行es

```dockerfile
docker run -d `
  --name es818 `
  -p 9200:9200 `
  -p 9300:9300 `
  -e "discovery.type=single-node" `
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" `
  -e "xpack.security.enabled=false" `
  docker.elastic.co/elasticsearch/elasticsearch:8.18.2
```

# 拉取并运行 Kibana 容器

```
docker run -d `
  --name kib818 `
  -p 5601:5601 `
  -e "ELASTICSEARCH_HOSTS=http://host.docker.internal:9200" `
  docker.elastic.co/kibana/kibana:8.18.2
```

# 安装 IK 中文分词器

```
# 进入 Elasticsearch 容器
docker exec -it es818 /bin/bash

# 安装 IK 分词器（注意版本号是 8.18.2）
bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.18.2/elasticsearch-analysis-ik-8.18.2.zip

# 退出容器
exit

# 重启 Elasticsearch 使插件生效
docker restart es818
```

# 拉取并运行rabbitmq

```
docker run -d `
  --name rabbitmq `
  -p 5672:5672 `
  -p 15672:15672 `
  rabbitmq:management
```

