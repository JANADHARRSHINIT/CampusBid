# ==============================================================================
# Comprehensive Verification Script for Wanted Items Smart Alerts & Guest Header
# ==============================================================================
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$rnd = Get-Random -Minimum 1000 -Maximum 9999
$emailA = "student_verifyA_$rnd@campus.edu"
$emailB = "student_verifyB_$rnd@campus.edu"
$pass = "SecretPass123!"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  CAMPUSBID FULL VERIFICATION TEST SUITE" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# ------------------------------------------------------------------------------
# Test 1 & 2: Authenticated student with no requests opens Wanted Alerts without errors
# ------------------------------------------------------------------------------
Write-Host "`n[Test 1 & 2] Authenticated student with no requests opens Wanted Alerts..." -ForegroundColor Yellow
$sessionA = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$regBodyA = @{
    fullName = "Alice Verification"
    email = $emailA
    password = $pass
    studentId = "STU-VERIFY-A-$rnd"
} | ConvertTo-Json

$regResA = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBodyA -ContentType "application/json" -WebSession $sessionA
Write-Host " -> Student A registered successfully: id=$($regResA.data.id), studentId=$($regResA.data.studentId)" -ForegroundColor Green

# Fetch Wanted Items for Student A (should be empty list, status 200, no error)
$itemsA_initial = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items" -Method Get -WebSession $sessionA
if ($itemsA_initial.success -eq $true -and $itemsA_initial.data.Count -eq 0) {
    Write-Host " -> PASSED: Student A sees 0 requests, proper empty state data, no errors!" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Expected empty data list, got: $($itemsA_initial | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# Test 3 & 4: Student creates a wanted-item request and it is saved in MySQL
# ------------------------------------------------------------------------------
Write-Host "`n[Test 3 & 4] Student A creates a wanted request for 'TI-84 Graphing Calculator'..." -ForegroundColor Yellow
$wantedReq = @{
    itemName = "TI-84 Graphing Calculator"
    category = "Electronics"
    maxBudget = 100.00
    preferredCondition = "GOOD"
    additionalRequirements = "Must include charger or charging cable"
} | ConvertTo-Json

$createdRes = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items" -Method Post -Body $wantedReq -ContentType "application/json" -WebSession $sessionA
$createdItem = $createdRes.data
if ($createdItem.id -and $createdItem.itemName -eq "TI-84 Graphing Calculator" -and $createdItem.status -eq "ACTIVE") {
    Write-Host " -> PASSED: Wanted item created: id=$($createdItem.id), maxBudget=₹$($createdItem.maxBudget), status=$($createdItem.status)" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Create request returned unexpected data: $($createdRes | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# Test 5: Refreshing/re-fetching preserves the request from MySQL
# ------------------------------------------------------------------------------
Write-Host "`n[Test 5] Simulating browser reload: Fetching /api/wanted-items for Student A..." -ForegroundColor Yellow
$itemsA_after = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items" -Method Get -WebSession $sessionA
$found = $itemsA_after.data | Where-Object { $_.id -eq $createdItem.id }
if ($found -and $found.itemName -eq "TI-84 Graphing Calculator") {
    Write-Host " -> PASSED: Request preserved in MySQL across reloads! (Count = $($itemsA_after.data.Count))" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Created item not found upon re-fetch!" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# Test 6: Pausing and reactivating a request updates status and counts
# ------------------------------------------------------------------------------
Write-Host "`n[Test 6] Pausing request id=$($createdItem.id)..." -ForegroundColor Yellow
$pauseRes = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items/$($createdItem.id)/status?status=PAUSED" -Method Patch -WebSession $sessionA
if ($pauseRes.data.status -eq "PAUSED") {
    Write-Host " -> PASSED: Request successfully PAUSED: status=$($pauseRes.data.status)" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Failed to pause request: $($pauseRes | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

Write-Host " Reactivating request id=$($createdItem.id)..." -ForegroundColor Yellow
$resumeRes = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items/$($createdItem.id)/status?status=ACTIVE" -Method Patch -WebSession $sessionA
if ($resumeRes.data.status -eq "ACTIVE") {
    Write-Host " -> PASSED: Request successfully REACTIVATED: status=$($resumeRes.data.status)" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Failed to reactivate request: $($resumeRes | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# Test 7: A matching listing generates the correct alert
# ------------------------------------------------------------------------------
Write-Host "`n[Test 7] Student B publishes matching listing for 'TI-84 Calculator' at ₹80 (Budget: ₹100)..." -ForegroundColor Yellow
$sessionB = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$regBodyB = @{
    fullName = "Bob Seller"
    email = $emailB
    password = $pass
    studentId = "STU-VERIFY-B-$rnd"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBodyB -ContentType "application/json" -WebSession $sessionB

$closing = (Get-Date).AddDays(3).ToString("yyyy-MM-ddTHH:mm:ss")
$listingReq = @{
    title = "Texas Instruments TI-84 Calculator Plus"
    description = "Includes charging cable, pristine condition"
    category = "Electronics"
    condition = "LIKE_NEW"
    startingPrice = 80.00
    minBidIncrement = 5.00
    closingTime = $closing
    sellerId = "STU-VERIFY-B-$rnd"
    sellerName = "Bob Seller"
    sellerEmail = $emailB
} | ConvertTo-Json

$listRes = Invoke-RestMethod -Uri "$baseUrl/api/listings" -Method Post -Body $listingReq -ContentType "application/json" -WebSession $sessionB
$listingId = $listRes.data.id
Write-Host " -> Listing created: id=$listingId, title='$($listRes.data.title)', startingPrice=₹$($listRes.data.startingPrice)" -ForegroundColor Green

Start-Sleep -Seconds 1

# Check Student A's wanted items now shows matching lot
$itemsA_matched = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items" -Method Get -WebSession $sessionA
$itemWithMatch = $itemsA_matched.data | Where-Object { $_.id -eq $createdItem.id }
if ($itemWithMatch.matches.Count -ge 1) {
    Write-Host " -> PASSED: Wanted item shows $($itemWithMatch.matches.Count) matching lots in dashboard!" -ForegroundColor Green
    $matchedLot = $itemWithMatch.matches | Where-Object { $_.listingId -eq $listingId }
    Write-Host "    Matched lot: id=$($matchedLot.listingId), title='$($matchedLot.listingTitle)', price=₹$($matchedLot.listingPrice)" -ForegroundColor Cyan
} else {
    Write-Host " -> FAILED: No matches found in wanted item response: $($itemWithMatch | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Check Student A's notifications
$notifsA = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -WebSession $sessionA
$matchNotif = $notifsA.data | Where-Object { $_.type -eq "WANTED_ITEM_MATCH" -and $_.relatedListingId -eq $listingId }
if ($matchNotif) {
    Write-Host " -> PASSED: In-app notification generated for Student A: '$($matchNotif.title)'" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Notification not found for listing $listingId" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# Test 8: Isolation - One student cannot view or modify another student's requests
# ------------------------------------------------------------------------------
Write-Host "`n[Test 8] Access Isolation: Student B attempts to modify and delete Student A's request..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/wanted-items/$($createdItem.id)" -Method Delete -WebSession $sessionB
    Write-Host " -> FAILED: Student B was able to delete Student A's request!" -ForegroundColor Red
    exit 1
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 403) {
        Write-Host " -> PASSED: Unauthorized delete correctly rejected with HTTP 403 Forbidden!" -ForegroundColor Green
    } else {
        Write-Host " -> FAILED: Expected HTTP 403, got $code" -ForegroundColor Red
        exit 1
    }
}

try {
    Invoke-RestMethod -Uri "$baseUrl/api/wanted-items/$($createdItem.id)" -Method Get -WebSession $sessionB
    Write-Host " -> FAILED: Student B was able to view Student A's request details!" -ForegroundColor Red
    exit 1
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 403) {
        Write-Host " -> PASSED: Unauthorized detail view correctly rejected with HTTP 403 Forbidden!" -ForegroundColor Green
    } else {
        Write-Host " -> FAILED: Expected HTTP 403, got $code" -ForegroundColor Red
        exit 1
    }
}

# ------------------------------------------------------------------------------
# Test 9: Expired/unauthenticated sessions return HTTP 401 (not 500)
# ------------------------------------------------------------------------------
Write-Host "`n[Test 9] Unauthenticated / expired session handling on /api/wanted-items..." -ForegroundColor Yellow
$guestSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
try {
    Invoke-RestMethod -Uri "$baseUrl/api/wanted-items" -Method Get -WebSession $guestSession
    Write-Host " -> FAILED: Unauthenticated request was allowed!" -ForegroundColor Red
    exit 1
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 401) {
        Write-Host " -> PASSED: Unauthenticated access correctly rejected with HTTP 401 Unauthorized!" -ForegroundColor Green
    } else {
        Write-Host " -> FAILED: Expected HTTP 401, got $code" -ForegroundColor Red
        exit 1
    }
}

# ------------------------------------------------------------------------------
# Test 10: Existing listings, bidding, and notifications still work
# ------------------------------------------------------------------------------
Write-Host "`n[Test 10] Verifying public listings, active bidding, and notification services..." -ForegroundColor Yellow
$publicRes = Invoke-RestMethod -Uri "$baseUrl/api/listings" -Method Get
$publicListings = if ($publicRes.data) { $publicRes.data } else { $publicRes }
if ($publicListings.Count -gt 0) {
    Write-Host " -> PASSED: Public listings browsing works ($($publicListings.Count) lots found)" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Public listings returned empty" -ForegroundColor Red
    exit 1
}

# Place a bid from Student A on Student B's listing
$bidBody = @{
    amount = 85.00
    bidderId = "STU-VERIFY-A-$rnd"
    bidderName = "Alice Verification"
} | ConvertTo-Json
$bidResRaw = Invoke-RestMethod -Uri "$baseUrl/api/listings/$listingId/bids" -Method Post -Body $bidBody -ContentType "application/json" -WebSession $sessionA
$bidRes = if ($bidResRaw.data) { $bidResRaw.data } else { $bidResRaw }
if ($bidRes.currentPrice -eq 85.00) {
    Write-Host " -> PASSED: Bidding works! Leading bid updated to ₹$($bidRes.currentPrice)" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Bid failed: $($bidResRaw | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Clean up Student A's test request
$delRes = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items/$($createdItem.id)" -Method Delete -WebSession $sessionA
Write-Host " -> Cleaned up test wanted item id=$($createdItem.id)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  ALL 10 VERIFICATION TESTS PASSED SUCCESSFULLY! " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
