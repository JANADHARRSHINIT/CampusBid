# CampusBid Guest Mode & Navigation Verification Test
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$rnd = Get-Random -Minimum 1000 -Maximum 9999
$email = "student_guest_test_$rnd@campus.edu"
$password = "StrongPass123!"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  CAMPUSBID GUEST MODE & RESTRICTED ACCESS TEST" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# -----------------------------------------------------------------------------
# Part 1: Verify Guest Permissions (Unauthenticated)
# -----------------------------------------------------------------------------
Write-Host "`n--- PART 1: GUEST PERMISSIONS (UNAUTHENTICATED) ---" -ForegroundColor Magenta

# 1.1 Guests can browse active auctions
Write-Host "`n[1.1] Guest browsing active auctions (GET /api/listings)..." -ForegroundColor Yellow
$activeListings = Invoke-RestMethod -Uri "$baseUrl/api/listings" -Method Get
Write-Host " -> PASSED: Active listings retrieved ($($activeListings.data.Count) lots found)" -ForegroundColor Green
$sampleLotId = $activeListings.data[0].id

# 1.2 Guests can search and filter public active listings
Write-Host "`n[1.2] Guest search and filter (GET /api/listings?category=Sports%20%26%20Outdoors)..." -ForegroundColor Yellow
$filtered = Invoke-RestMethod -Uri "$baseUrl/api/listings?category=Sports%20%26%20Outdoors" -Method Get
Write-Host " -> PASSED: Filtered active listings retrieved ($($filtered.data.Count) lots found)" -ForegroundColor Green

# 1.3 Guests can view auction details
Write-Host "`n[1.3] Guest viewing single auction details (GET /api/listings/$sampleLotId)..." -ForegroundColor Yellow
$lotDetail = Invoke-RestMethod -Uri "$baseUrl/api/listings/$sampleLotId" -Method Get
Write-Host " -> PASSED: Public auction details accessible (Title: '$($lotDetail.data.title)', Price: `$$($lotDetail.data.currentPrice))" -ForegroundColor Green

# 1.4 Guests can view public bid history
Write-Host "`n[1.4] Guest viewing public bid history (GET /api/listings/$sampleLotId/bids)..." -ForegroundColor Yellow
$bidHistory = Invoke-RestMethod -Uri "$baseUrl/api/listings/$sampleLotId/bids" -Method Get
Write-Host " -> PASSED: Public bid history accessible ($($bidHistory.data.Count) bids recorded)" -ForegroundColor Green

# -----------------------------------------------------------------------------
# Part 2: Verify Protected Actions Are Rejected for Guests
# -----------------------------------------------------------------------------
Write-Host "`n--- PART 2: PROTECTED ACTIONS REJECTED FOR GUESTS ---" -ForegroundColor Magenta

# 2.1 Guests cannot place bids
Write-Host "`n[2.1] Guest attempts to place a bid (POST /api/listings/$sampleLotId/bids)..." -ForegroundColor Yellow
try {
    $bidBody = @{ amount = 9999.00 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/listings/$sampleLotId/bids" -Method Post -Body $bidBody -ContentType "application/json"
    Write-Host " -> FAILED: Guest was able to place a bid!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Bidding denied with HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

# 2.2 Guests cannot create listings
Write-Host "`n[2.2] Guest attempts to create a listing (POST /api/listings)..." -ForegroundColor Yellow
try {
    $listingBody = @{
        title = "Unauthorized Item"
        category = "Electronics"
        description = "Should not be created"
        condition = "NEW"
        startingPrice = 100.00
        minBidIncrement = 10.00
        closingTime = (Get-Date).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/listings" -Method Post -Body $listingBody -ContentType "application/json"
    Write-Host " -> FAILED: Guest was able to create a listing!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Listing creation denied with HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

# 2.3 Guests cannot access private dashboards (pending-decisions, won, my-listings)
Write-Host "`n[2.3] Guest attempts to access private seller decisions (GET /api/listings/pending-decisions)..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/listings/pending-decisions" -Method Get
    Write-Host " -> FAILED: Guest accessed pending decisions!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Pending decisions denied with HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

Write-Host "`n[2.4] Guest attempts to access won listings (GET /api/listings/won)..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/listings/won" -Method Get
    Write-Host " -> FAILED: Guest accessed won listings!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Won listings denied with HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

Write-Host "`n[2.5] Guest attempts to access notifications (GET /api/notifications)..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get
    Write-Host " -> FAILED: Guest accessed notifications!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Notifications denied with HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

Write-Host "`n[2.6] Guest attempts to access wanted items (GET /api/wanted-items)..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/wanted-items" -Method Get
    Write-Host " -> FAILED: Guest accessed wanted items!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Wanted items denied with HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

# -----------------------------------------------------------------------------
# Part 3: Authenticated Flow (Full Access) & Immediate Logout
# -----------------------------------------------------------------------------
Write-Host "`n--- PART 3: AUTHENTICATED FLOW & LOGOUT TO GUEST MODE ---" -ForegroundColor Magenta

# 3.1 Register and Login
Write-Host "`n[3.1] Registering student account ($email)..." -ForegroundColor Yellow
$regBody = @{
    fullName = "Taylor Swift"
    email = $email
    password = $password
    studentId = "STU-TAY-$rnd"
} | ConvertTo-Json

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$regRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json" -WebSession $session
Write-Host " -> Registered student id=$($regRes.data.id), role=$($regRes.data.role)" -ForegroundColor Green

# 3.2 Verify authenticated session persistence (/api/auth/me)
Write-Host "`n[3.2] Checking /api/auth/me session persistence..." -ForegroundColor Yellow
$me = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -WebSession $session
Write-Host " -> PASSED: Authenticated session confirmed for '$($me.data.fullName)'" -ForegroundColor Green

# 3.3 Authenticated student accesses notifications and dashboards
Write-Host "`n[3.3] Authenticated student accesses notifications..." -ForegroundColor Yellow
$notifs = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -WebSession $session
Write-Host " -> PASSED: Notifications accessible for authenticated student ($($notifs.data.Count) found)" -ForegroundColor Green

Write-Host "`n[3.4] Authenticated student accesses wanted items..." -ForegroundColor Yellow
$wanted = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items" -Method Get -WebSession $session
Write-Host " -> PASSED: Wanted items accessible for authenticated student ($($wanted.data.Count) found)" -ForegroundColor Green

# 3.5 Logout
Write-Host "`n[3.5] Logging out student..." -ForegroundColor Yellow
$logoutRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/logout" -Method Post -WebSession $session
Write-Host " -> PASSED: Logout successful ($($logoutRes.message))" -ForegroundColor Green

# 3.6 Verify session destroyed and returned to guest mode
Write-Host "`n[3.6] Checking /api/auth/me after logout..." -ForegroundColor Yellow
$meAfter = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -WebSession $session
if ($meAfter.data -eq $null) {
    Write-Host " -> PASSED: Session cleared, returned to guest mode (me.data is null)" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: User profile still returned after logout! ($($meAfter.data | ConvertTo-Json))" -ForegroundColor Red
    exit 1
}

# 3.7 Verify subsequent protected request returns 401
Write-Host "`n[3.7] Subsequent protected request after logout (GET /api/notifications)..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -WebSession $session
    Write-Host " -> FAILED: Protected notifications accessible after logout!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Protected request rejected with HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  ALL GUEST MODE & AUTHENTICATION TESTS PASSED! 🎉" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
