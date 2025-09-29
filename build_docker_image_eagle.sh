#!/bin/bash
SWR=swr.ae-ad-1.g42cloud.com
REPO_GIT_HELPER=$SWR/r100/platform/r100/git-helper

docker login -u ae-ad-1_r100_platform@${eagle_docker_login_access_key} -p ${eagle_docker_login_password} $SWR

COMMIT=${gitlabAfter:-${GIT_COMMIT}}
BRANCH=${gitlabTargetBranch:-${GIT_BRANCH}}
TAG_CM=${COMMIT:0:8}
TAG_BR=${BRANCH##*/}
TAG=${TAG_BR}-${TAG_CM}
echo "tag: $TAG"
docker build --build-arg EXEC_API=git-helper -t ${REPO_GIT_HELPER}:$TAG .

if test -n "${gitlabMergeRequestId}";then
	echo "Skip docker push in a merge request"
else
	docker push ${REPO_GIT_HELPER}:$TAG
fi
