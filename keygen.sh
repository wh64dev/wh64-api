#!/bin/bash
echo "GENERATING JWT SECRET KEY..."
KEY=$(openssl rand -hex 25)

echo "Your new secret key: $KEY"
echo "$KEY" > secretkey
