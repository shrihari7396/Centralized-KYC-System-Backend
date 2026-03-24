// MongoDB Seed Data for kyc_db
// Usage: mongosh "mongodb://root:899961@localhost:27017/kyc_db?authSource=admin" seed_data.js

const dbName = 'kyc_db';
const db = db.getSiblingDB(dbName);

print(`Using database: ${dbName}`);

// Clear existing dummy data if needed (Optional, uncomment to use)
// db.banks.deleteMany({ code: 'CB001' });
// db.bank_customers.deleteMany({ bankId: 'bank_id_101' });
// db.consents.deleteMany({ bankId: 'bank_id_101' });
// db.bank_customer_kyc_data.deleteMany({ bankId: 'bank_id_101' });

// 1. Seed Banks
const bankId = 'bank_id_101';
const bank = {
    _id: bankId,
    name: 'Central Bank',
    code: 'CB001',
    username: 'central_bank_admin', // Use this for staff login/profile
    createdAt: new Date()
};

db.banks.replaceOne({ _id: bank._id }, bank, { upsert: true });
print('Seeded Banks');

// 2. Seed Consents
const consents = [
    {
        _id: 'consent_001',
        bankId: bankId,
        userId: 'user_001',
        status: 'ACTIVE',
        requestedDocuments: ['AADHAAR', 'PAN'],
        createdAt: new Date('2026-03-20T10:00:00Z'),
        updatedAt: new Date('2026-03-20T11:00:00Z')
    },
    {
        _id: 'consent_002',
        bankId: bankId,
        userId: 'user_002',
        status: 'ACTIVE_OVERRIDE',
        requestedDocuments: ['AADHAAR', 'DRIVING_LICENSE'],
        createdAt: new Date('2026-03-21T09:00:00Z'),
        updatedAt: new Date('2026-03-21T10:00:00Z')
    },
    {
        _id: 'consent_003',
        bankId: bankId,
        userId: 'user_003',
        status: 'CONSENT_PENDING',
        requestedDocuments: ['PAN', 'ADDRESS_PROOF'],
        createdAt: new Date('2026-03-23T08:00:00Z'),
        updatedAt: new Date('2026-03-23T08:00:00Z')
    },
    {
        _id: 'consent_004',
        bankId: bankId,
        userId: 'user_004',
        status: 'KYC_PENDING',
        requestedDocuments: ['AADHAAR', 'INCOME_PROOF'],
        createdAt: new Date('2026-03-23T12:00:00Z'),
        updatedAt: new Date('2026-03-23T12:30:00Z')
    },
    {
        _id: 'consent_005',
        bankId: bankId,
        userId: 'user_005',
        status: 'KYC_REJECTED',
        requestedDocuments: ['PAN'],
        createdAt: new Date('2026-03-22T14:00:00Z'),
        updatedAt: new Date('2026-03-22T15:00:00Z')
    }
];

consents.forEach(c => {
    db.consents.replaceOne({ _id: c._id }, c, { upsert: true });
});
print('Seeded Consents');

// 3. Seed Bank Customers
const customers = [
    {
        _id: 'cust_001',
        userId: 'user_001',
        bankId: bankId,
        consentIds: ['consent_001'],
        kycRequestId: 1001,
        riskLevel: 'LOW',
        overrideReason: null,
        verifiedAt: new Date('2026-03-20T11:00:00Z'),
        createdAt: new Date('2026-03-19T09:00:00Z')
    },
    {
        _id: 'cust_002',
        userId: 'user_002',
        bankId: bankId,
        consentIds: ['consent_002'],
        kycRequestId: 1002,
        riskLevel: 'MEDIUM',
        overrideReason: 'Documents verified manually via branch visit',
        verifiedAt: new Date('2026-03-21T10:00:00Z'),
        createdAt: new Date('2026-03-20T08:00:00Z')
    },
    {
        _id: 'cust_003',
        userId: 'user_003',
        bankId: bankId,
        consentIds: ['consent_003'],
        kycRequestId: null,
        riskLevel: 'HIGH', // Pending usually starts with unknown or high risk
        overrideReason: null,
        verifiedAt: null,
        createdAt: new Date('2026-03-23T08:00:00Z')
    },
    {
        _id: 'cust_004',
        userId: 'user_004',
        bankId: bankId,
        consentIds: ['consent_004'],
        kycRequestId: 1004,
        riskLevel: 'LOW',
        overrideReason: null,
        verifiedAt: null,
        createdAt: new Date('2026-03-23T12:00:00Z')
    },
    {
        _id: 'cust_005',
        userId: 'user_005',
        bankId: bankId,
        consentIds: ['consent_005'],
        kycRequestId: 1005,
        riskLevel: 'HIGH',
        overrideReason: null,
        verifiedAt: null,
        createdAt: new Date('2026-03-22T14:00:00Z')
    }
];

customers.forEach(cust => {
    db.bank_customers.replaceOne({ _id: cust._id }, cust, { upsert: true });
});
print('Seeded Bank Customers');

// 4. Seed KYC Data (For Active/Override Customers)
const kycData = [
    {
        _id: 'kyc_data_001',
        userId: 'user_001',
        bankId: bankId,
        consentId: 'consent_001',
        kycData: {
            personal: {
                firstName: 'John',
                lastName: 'Doe',
                dob: '1990-05-15',
                gender: 'M',
                phone: '+1234567890',
                email: 'john.doe@example.com'
            },
            address: {
                street: '123 Tech Lane',
                city: 'Silicon Valley',
                state: 'CA',
                country: 'USA',
                zip: '94025'
            },
            documents: {
                AADHAAR: '1234-5678-9012',
                PAN: 'ABCDE1234F'
            }
        },
        createdAt: new Date('2026-03-20T11:00:00Z')
    },
    {
        _id: 'kyc_data_002',
        userId: 'user_002',
        bankId: bankId,
        consentId: 'consent_002',
        kycData: {
            personal: {
                firstName: 'Jane',
                lastName: 'Smith',
                dob: '1985-11-20',
                gender: 'F',
                phone: '+1987654321',
                email: 'jane.smith@example.com'
            },
            address: {
                street: '456 Innovation Blvd',
                city: 'New York',
                state: 'NY',
                country: 'USA',
                zip: '10001'
            },
            documents: {
                AADHAAR: '9876-5432-1098',
                DRIVING_LICENSE: 'NY-DL-999999'
            }
        },
        createdAt: new Date('2026-03-21T10:00:00Z')
    }
];

kycData.forEach(data => {
    db.bank_customer_kyc_data.replaceOne({ _id: data._id }, data, { upsert: true });
});
print('Seeded KYC Data');

print('--- SEEDING COMPLETE ---');
print('Summary of seeded data:');
print(`Banks: ${db.banks.countDocuments({ _id: bankId })}`);
print(`Customers: ${db.bank_customers.countDocuments({ bankId: bankId })}`);
print(`Consents: ${db.consents.countDocuments({ bankId: bankId })}`);
print(`KYC Data Docs: ${db.bank_customer_kyc_data.countDocuments({ bankId: bankId })}`);
