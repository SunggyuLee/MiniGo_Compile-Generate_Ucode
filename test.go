var p int
var q int
func main() {
	var i int
	var j int
	var k int
	var rem int
	var sum int
	i int = 2
	while i <= 500 {
		sum = 0
		k = i/2
		j = i
		for j <= k {
			rem = i%j
			if rem == 0 {
				sum = sum+j
			}
			++j
		}
		if i == sum {
			fmt.write(i)
		}
		++i
	}
}