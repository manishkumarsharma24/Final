output "eks_cluster_endpoint" { value = module.eks.cluster_endpoint }
output "rds_endpoint"         { value = aws_db_instance.postgres.endpoint }
output "redis_endpoint"       { value = aws_elasticache_replication_group.redis.primary_endpoint_address }
