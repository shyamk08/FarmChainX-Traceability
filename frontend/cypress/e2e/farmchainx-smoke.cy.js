describe('FarmChainX — Full Journey Smoke Test', () => {
  it('Farmer → Distributor → Retailer → Consumer → Verified', () => {
    cy.visit('http://localhost:4200/verify/11111111-1111-1111-1111-111111111111')
    cy.contains('Basmati Rice', { timeout: 10000 })
    cy.contains('100% Blockchain Verified')

    cy.visit('http://localhost:4200/login')
    cy.get('input[type="email"]').type('distributor@farmchainx.com')
    cy.get('input[type="password"]').type('1234')
    cy.contains('Login').click()

    cy.visit('http://localhost:4200/verify/11111111-1111-1111-1111-111111111111')
    cy.contains('Confirm Receipt from Farmer').click()
    cy.contains('FINAL HANDOVER').click()
    cy.get('select').select(0)
    cy.get('input[placeholder*="Location"]').type('Mumbai Warehouse')
    cy.contains('HAND OVER TO RETAILER').click()
    cy.contains('Success!')

    cy.visit('http://localhost:4200/login')
    cy.get('input[type="email"]').type('consumer@farmchainx.com')
    cy.get('input[type="password"]').type('1234')
    cy.contains('Login').click()

    cy.visit('http://localhost:4200/verify/11111111-1111-1111-1111-111111111111')
    cy.get('input[type="number"]').clear().type('5')
    cy.get('textarea').type('Best rice ever!')
    cy.contains('Submit Feedback').click()
    cy.contains('Thanks', { timeout: 10000 })
  })
})