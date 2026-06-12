variable "aws_region"        { default = "us-east-1" }
variable "db_instance_class" { default = "db.t3.medium" }
variable "db_username"       { default = "shopverse" }
variable "db_password"       { sensitive = true }
variable "eks_node_type"     { default = "t3.large" }
