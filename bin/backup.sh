#!/bin/bash
dd if=$1 bs=1M status=progress | pigz | openssl enc -e -aes-256-cbc -salt -out $2
