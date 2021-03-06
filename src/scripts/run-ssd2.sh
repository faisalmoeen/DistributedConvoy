#!/usr/bin/env bash


DATASETS=(trucks-d brink-d tdrive-d)
declare -A time_offsets
time_offsets[trucks-d_start]=2
time_offsets[trucks-d_end]=2875
time_offsets[brink-d_start]=2
time_offsets[brink-d_end]=20000
time_offsets[tdrive-d_start]=0
time_offsets[tdrive-d_end]=53328
DCM_MR_EXP_RESULT=dcm_mr_exps.txt
CORES=(16 14 12 10 8 6 4 2)
IN_DIR=/home/fmo/datasets
OUT_DIR=/home/fmo/output


#===============================================DCM_MR============================================

E=(0.0006 0.00006 0.000006) #eps for DBSCAN
M=(3 6 9) #min num of objs in a cluster
K=(200 400 600 800 1000 1200)


rm ${DCM_MR_EXP_RESULT}
touch ${DCM_MR_EXP_RESULT}
echo "dataset,e,m,cores,time(ms)" >> ${DCM_MR_EXP_RESULT}

for dataset in ${DATASETS[@]}
do
for m in ${M[@]}
do
for e in ${E[@]}
do
for k in ${K[@]}
do
    start=$(date +%s%3N)
    echo $dataset,${e},${m},${k}
    hdfs dfs -rm -r ${OUT_DIR}/${dataset}/${m}_${k}_${e}
    hadoop jar ../../target/DistributedConvoy-0.0.1-SNAPSHOT.jar mapreduce.ConvoyJobNlognMRSplits ${IN_DIR}/${dataset} ${OUT_DIR}/${dataset}/${m}_${k}_${e} \
    ${m} ${k} ${e} ${time_offsets[${dataset}_start]} ${time_offsets[${dataset}_end]}
    end=$(date +%s%3N)
    echo $dataset,${e},${m},${k},${core},$(($end-$start))
    echo "$dataset,${e},${m},${k},${core},$(($end-$start))" >> ${DCM_MR_EXP_RESULT}
done
done
done
done


#java -cp target/DistributedConvoy-0.0.1-SNAPSHOT.jar mapreduce.ConvoyJobNlognMRSplits /user/faisal/datasets/trucks-d /user/faisal/output/dcm-mr/trucks-d
#hadoop jar target/DistributedConvoy-0.0.1-SNAPSHOT.jar mapreduce.ConvoyJobNlognMRSplits /user/faisal/datasets/trucks-d /user/faisal/output/trucks-d/DCM 6 180 0.00006 2 2875
