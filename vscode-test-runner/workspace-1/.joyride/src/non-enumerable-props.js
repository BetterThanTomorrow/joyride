// Description: For testing completions/properties on an object with non-enumerable properties

const obj = {};

// Create a non-enumerable property
Object.defineProperty(obj, 'x', {
  value: 10,
  writable: true,
  enumerable: false,
  configurable: true
});

// Create a non-enumerable property with a getter
Object.defineProperty(obj, 'y', {
  get: function() { return this._y; },
  set: function(value) { this._y = value },
  enumerable: false,
  configurable: true
});

// Create a non-enumerable property with a setter
Object.defineProperty(obj, 'z', {
  get: function() { return this._z },
  set: function(value) { this._z = value },
  enumerable: false,
  configurable: true
});

module.exports = { obj }