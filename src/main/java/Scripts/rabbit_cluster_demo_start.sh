#!/bin/bash

docker-compose up -d

sleep 3

docker exec scripts_rabbit1_1 rabbitmq-plugins enable rabbitmq_management

docker exec scripts_rabbit2_1 rabbitmq-plugins enable rabbitmq_management

docker exec scripts_rabbit3_1 rabbitmq-plugins enable rabbitmq_management


sleep 1

docker exec scripts_rabbit2_1 bash -c "rabbitmqctl stop_app && rabbitmqctl join_cluster rabbit@rabbit1 && rabbitmqctl start_app "

docker exec scripts_rabbit3_1 bash -c "rabbitmqctl stop_app && rabbitmqctl join_cluster rabbit@rabbit1 && rabbitmqctl start_app "


echo Cluster successfully created!