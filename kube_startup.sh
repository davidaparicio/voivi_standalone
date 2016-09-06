#!/bin/bash
export CLOUDSDK_CLUSTER_NAME="cluster"

gcloud compute disks create --zone=$CLOUDSDK_COMPUTE_ZONE --size=5GB mongo-disk
gcloud container clusters create --machine-type=n1-standard-1 --zone=$CLOUDSDK_COMPUTE_ZONE $CLOUDSDK_CLUSTER_NAME
gcloud container clusters get-credentials $CLOUDSDK_CLUSTER_NAME
