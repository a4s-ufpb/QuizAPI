<#
.SYNOPSIS
    Popula o banco Postgres (rodando via Docker) com dados de teste.

.DESCRIPTION
    Insere, via `docker exec ... psql`, um usuário admin, um usuário comum,
    um tema, questões, alternativas, respostas, score e estatística de
    conclusão. Cada inserção verifica antes se o dado já existe (por nome,
    email ou combinação de chaves) e pula caso positivo — o script pode ser
    rodado várias vezes sem duplicar dados.

.USAGE
    Suba o Postgres (docker compose up -d db) e rode:
        ./seed.ps1

    Se o container/usuário/banco tiverem nomes diferentes do padrão do
    docker-compose.yaml deste repo, passe os parâmetros:
        ./seed.ps1 -Container meu-container -PgUser meu-user -PgDb meu-db

.NOTES
    Credenciais criadas para teste de login:
      admin@quizapp.com / Admin@123 (ADMIN)
      user1@quizapp.com / User@123  (USER)
#>

param(
    [string]$Container = "container-database-quiz",
    [string]$PgUser = "quiz",
    [string]$PgDb = "quizdb"
)

$ErrorActionPreference = "Stop"

function Invoke-Psql {
    param([Parameter(Mandatory = $true)][string]$Sql)

    $result = docker exec -i $Container psql -U $PgUser -d $PgDb -v ON_ERROR_STOP=1 -tAc $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao executar SQL no container '$Container': $Sql"
    }
    # Em INSERT ... RETURNING, o psql imprime o valor retornado e, em outra
    # linha, o tag de status ("INSERT 0 1") mesmo em modo -tA — fica só a 1ª linha.
    # Uso @(...) para forçar contexto de array mesmo quando só há 1 linha
    # (senão o PowerShell trata a string como escalar e "$lines[0]" indexa
    # caractere a caractere em vez de pegar a linha inteira).
    $lines = @($result) | Where-Object { $_ -and $_.Trim() -ne "" }
    if ($lines.Count -eq 0) { return "" }
    return ($lines | Select-Object -First 1).ToString().Trim()
}

function Escape-Sql {
    param([string]$Value)
    if ($null -eq $Value) { return "" }
    return $Value.Replace("'", "''")
}

Write-Host "Verificando conexão com o container '$Container'..." -ForegroundColor Cyan
docker exec $Container pg_isready -U $PgUser -d $PgDb | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Não foi possível conectar ao Postgres no container '$Container'." -ForegroundColor Red
    Write-Host "Confirme se o container está rodando (docker compose up -d db) e se os parâmetros -Container/-PgUser/-PgDb estão corretos." -ForegroundColor Red
    exit 1
}
Write-Host "Conectado. Iniciando seed..." -ForegroundColor Cyan
Write-Host ""

# --- Hashes BCrypt pré-gerados (compatíveis com o BCryptPasswordEncoder da API) ---
$adminPasswordHash = '$2a$10$hAq1Qffrd5hFzplQH1bCvOLIqvkreNX5OfaWq/bTds/tWbTpPDrV.'
$userPasswordHash = '$2a$10$D.YyjgTfh7zwABv6VWMms.9HJPf3UWGgWT7h9aupAX7a24YA1JG8e'

# --- Usuário admin ---
$adminEmail = "admin@quizapp.com"
$adminUuid = Invoke-Psql "SELECT uuid FROM tb_user WHERE email = '$adminEmail'"
if ([string]::IsNullOrWhiteSpace($adminUuid)) {
    $adminUuid = Invoke-Psql "INSERT INTO tb_user (uuid, name, email, password, role) VALUES (gen_random_uuid(), 'Admin Seed', '$adminEmail', '$adminPasswordHash', 'ADMIN') RETURNING uuid"
    Write-Host "[+] Usuário admin criado ($adminEmail / Admin@123)" -ForegroundColor Green
} else {
    Write-Host "[=] Usuário admin já existe, pulando." -ForegroundColor Yellow
}

# --- Usuário comum ---
$userEmail = "user1@quizapp.com"
$userUuid = Invoke-Psql "SELECT uuid FROM tb_user WHERE email = '$userEmail'"
if ([string]::IsNullOrWhiteSpace($userUuid)) {
    $userUuid = Invoke-Psql "INSERT INTO tb_user (uuid, name, email, password, role) VALUES (gen_random_uuid(), 'Usuário Seed', '$userEmail', '$userPasswordHash', 'USER') RETURNING uuid"
    Write-Host "[+] Usuário comum criado ($userEmail / User@123)" -ForegroundColor Green
} else {
    Write-Host "[=] Usuário comum já existe, pulando." -ForegroundColor Yellow
}

# --- Tema ---
$themeName = "Seed - Geografia"
$themeId = Invoke-Psql "SELECT id FROM tb_theme WHERE name = '$themeName'"
if ([string]::IsNullOrWhiteSpace($themeId)) {
    $themeId = Invoke-Psql "INSERT INTO tb_theme (name, image_url, creator_uuid) VALUES ('$themeName', 'https://picsum.photos/seed/quiz-geo/400', '$adminUuid') RETURNING id"
    Write-Host "[+] Tema '$themeName' criado (id=$themeId)" -ForegroundColor Green
} else {
    Write-Host "[=] Tema '$themeName' já existe (id=$themeId), pulando." -ForegroundColor Yellow
}

# --- Questões + Alternativas ---
# Cada item: título da questão, URL de imagem, lista de (texto, correta)
$questionsData = @(
    @{
        Title = "Qual é a capital do Brasil?"
        Alternatives = @(
            @{ Text = "Brasília"; Correct = $true },
            @{ Text = "São Paulo"; Correct = $false },
            @{ Text = "Rio de Janeiro"; Correct = $false },
            @{ Text = "Salvador"; Correct = $false }
        )
    },
    @{
        Title = "Qual é o maior oceano do mundo?"
        Alternatives = @(
            @{ Text = "Oceano Atlântico"; Correct = $false },
            @{ Text = "Oceano Índico"; Correct = $false },
            @{ Text = "Oceano Pacífico"; Correct = $true },
            @{ Text = "Oceano Ártico"; Correct = $false }
        )
    }
)

# id da primeira alternativa correta da primeira questão, usado depois na Response
$firstQuestionId = $null
$firstCorrectAlternativeId = $null

foreach ($q in $questionsData) {
    $title = Escape-Sql $q.Title
    $questionId = Invoke-Psql "SELECT id FROM tb_question WHERE title = '$title' AND theme_id = $themeId"

    if ([string]::IsNullOrWhiteSpace($questionId)) {
        $questionId = Invoke-Psql "INSERT INTO tb_question (title, image_url, images_order, theme_id, creator_uuid) VALUES ('$title', 'https://picsum.photos/seed/$([guid]::NewGuid().ToString('N').Substring(0,8))/400', 'URL', $themeId, '$adminUuid') RETURNING id"
        Write-Host "[+] Questão '$($q.Title)' criada (id=$questionId)" -ForegroundColor Green

        foreach ($alt in $q.Alternatives) {
            $altText = Escape-Sql $alt.Text
            $correctSql = if ($alt.Correct) { "true" } else { "false" }
            $altId = Invoke-Psql "INSERT INTO tb_alternative (text, correct, question_id) VALUES ('$altText', $correctSql, $questionId) RETURNING id"

            if ($alt.Correct -and $null -eq $firstCorrectAlternativeId) {
                $firstCorrectAlternativeId = $altId
                $firstQuestionId = $questionId
            }
        }
        Write-Host "    -> $($q.Alternatives.Count) alternativas inseridas" -ForegroundColor Green
    } else {
        Write-Host "[=] Questão '$($q.Title)' já existe (id=$questionId), pulando." -ForegroundColor Yellow

        if ($null -eq $firstCorrectAlternativeId) {
            $firstCorrectAlternativeId = Invoke-Psql "SELECT id FROM tb_alternative WHERE question_id = $questionId AND correct = true LIMIT 1"
            $firstQuestionId = $questionId
        }
    }
}

# --- Response (usuário comum respondendo a 1ª questão, modo single player) ---
if ($firstQuestionId -and $firstCorrectAlternativeId) {
    $responseId = Invoke-Psql "SELECT id FROM tb_response WHERE user_uuid = '$userUuid' AND question_id = $firstQuestionId"
    if ([string]::IsNullOrWhiteSpace($responseId)) {
        Invoke-Psql "INSERT INTO tb_response (date_time, user_uuid, question_id, alternative_id, game_mode) VALUES (CURRENT_DATE, '$userUuid', $firstQuestionId, $firstCorrectAlternativeId, 'SINGLE_PLAYER') RETURNING id" | Out-Null
        Write-Host "[+] Response criada (usuário comum respondeu a 1ª questão corretamente)" -ForegroundColor Green
    } else {
        Write-Host "[=] Response do usuário comum para a 1ª questão já existe, pulando." -ForegroundColor Yellow
    }
}

# --- Score ---
$scoreId = Invoke-Psql "SELECT id FROM tb_score WHERE user_uuid = '$userUuid' AND theme_id = $themeId"
if ([string]::IsNullOrWhiteSpace($scoreId)) {
    Invoke-Psql "INSERT INTO tb_score (result, user_uuid, theme_id) VALUES (185.68, '$userUuid', $themeId) RETURNING id" | Out-Null
    Write-Host "[+] Score criado para o usuário comum no tema '$themeName'" -ForegroundColor Green
} else {
    Write-Host "[=] Score do usuário comum para o tema '$themeName' já existe, pulando." -ForegroundColor Yellow
}

# --- StatisticPerConclusion ---
$studentName = "Usuário Seed"
$statisticId = Invoke-Psql "SELECT id FROM tb_statistic WHERE student_name = '$studentName' AND theme_name = '$themeName'"
if ([string]::IsNullOrWhiteSpace($statisticId)) {
    Invoke-Psql "INSERT INTO tb_statistic (creator_id, student_name, theme_name, percentagem_of_hits, date) VALUES ('$adminUuid', '$studentName', '$themeName', 80.0, CURRENT_DATE) RETURNING id" | Out-Null
    Write-Host "[+] Estatística de conclusão criada para '$studentName' no tema '$themeName'" -ForegroundColor Green
} else {
    Write-Host "[=] Estatística de conclusão para '$studentName' no tema '$themeName' já existe, pulando." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Seed concluído." -ForegroundColor Cyan
