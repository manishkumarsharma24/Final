# ShopFlow — Rename AWS service folders with deployment order prefix
$base = "F:\Final\AWS Project"

$renames = @(
    @("IAM",           "01_IAM"),
    @("VPC",           "02_VPC"),
    @("S3",            "03_S3"),
    @("SecretsManager","04_SecretsManager"),
    @("SSM",           "05_SSM"),
    @("RDS",           "06_RDS"),
    @("ElastiCache",   "07_ElastiCache"),
    @("EC2",           "08_EC2"),
    @("ALB",           "09_ALB"),
    @("ECR",           "10_ECR"),
    @("ECS",           "11_ECS"),
    @("SQS",           "12_SQS"),
    @("SNS",           "13_SNS"),
    @("Lambda",        "14_Lambda"),
    @("DynamoDB",      "15_DynamoDB"),
    @("Monitoring",    "16_Monitoring"),
    @("CICD",          "17_CICD")
)

foreach ($pair in $renames) {
    $old = Join-Path $base $pair[0]
    $new = $pair[1]
    if (Test-Path $old) {
        Rename-Item -Path $old -NewName $new
        Write-Host "Renamed: $($pair[0]) -> $new"
    } else {
        Write-Host "Skipped (not found): $($pair[0])"
    }
}

Write-Host "`nDone! All folders renamed."
