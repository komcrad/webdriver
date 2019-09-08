#!/bin/bash
openssl enc -aes-256-cbc -d -in $1 | pigz -d | dd of=$2 bs=1M status=progress
